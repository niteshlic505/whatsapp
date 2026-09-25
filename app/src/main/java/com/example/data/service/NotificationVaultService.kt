package com.example.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.AppDatabase
import com.example.data.model.NotificationVaultEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AppRepository
import com.example.data.security.VaultSecurityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationVaultService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: AppRepository
    private lateinit var preferencesRepository: UserPreferencesRepository

    // In-memory sliding cache to filter immediate duplicate notification events (e.g. within 20 seconds)
    private val recentProcessedSignatures = java.util.concurrent.ConcurrentHashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(applicationContext)
        val sec = VaultSecurityManager(applicationContext)
        repository = AppRepository(applicationContext, db, sec)
        preferencesRepository = UserPreferencesRepository(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        // Capture only WhatsApp and WhatsApp Business
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extract Title (individual contact or group title)
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
        val ticker = notification.tickerText?.toString()?.trim() ?: ""

        val rawSender = when {
            !conversationTitle.isNullOrBlank() && rawTitle.isNotBlank() && rawTitle != conversationTitle -> "$conversationTitle • $rawTitle"
            !conversationTitle.isNullOrBlank() -> conversationTitle
            rawTitle.isNotBlank() -> rawTitle
            !subText.isNullOrBlank() -> subText
            else -> "WhatsApp Contact"
        }

        // Clean sender to remove WhatsApp badges like "(2 messages)" or "(1 message)"
        val displaySender = normalizeSender(rawSender)
        val cleanSender = normalizeSender(rawTitle.ifBlank { rawSender })

        // Ignore generic system notifications like "Checking for new messages" or "WhatsApp Web is active"
        if (displaySender.contains("WhatsApp Web", ignoreCase = true) ||
            (displaySender.contains("WhatsApp", ignoreCase = true) && rawTitle.contains("running", ignoreCase = true)) ||
            rawTitle.contains("Backup in progress", ignoreCase = true)
        ) {
            return
        }

        val timestamp = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis()

        // Extract message content
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        val messagesToProcess = mutableListOf<String>()

        if (!textLines.isNullOrEmpty()) {
            for (line in textLines) {
                val str = line?.toString()?.trim() ?: continue
                if (str.isNotBlank()) {
                    messagesToProcess.add(str)
                }
            }
        } else {
            val singleText = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim() ?: ""
            if (singleText.isNotBlank()) {
                messagesToProcess.add(singleText)
            }
        }

        // Also check if ticker contains deletion notice
        val anyFieldHasDeletion = isDeletionNotice(rawTitle) ||
                isDeletionNotice(ticker) ||
                messagesToProcess.any { isDeletionNotice(it) }

        if (messagesToProcess.isEmpty() && !anyFieldHasDeletion) return

        serviceScope.launch {
            try {
                val excluded = preferencesRepository.excludedChats.first()
                if (excluded.contains(rawTitle) ||
                    excluded.contains(cleanSender) ||
                    (!conversationTitle.isNullOrBlank() && excluded.contains(conversationTitle))
                ) {
                    return@launch
                }

                if (anyFieldHasDeletion) {
                    // Mark recent messages from this sender as deleted (look back up to 48 hours)
                    val lookbackWindow = timestamp - (48 * 3600 * 1000L)
                    val marked = repository.markDeletedSmart(displaySender, cleanSender, lookbackWindow)
                    if (marked) {
                        notifyUserOfRecoveredDeletedMessage(cleanSender.ifBlank { displaySender })
                    }
                    return@launch
                }

                // Clean sliding cache of items older than 30 seconds
                val now = System.currentTimeMillis()
                val iterator = recentProcessedSignatures.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (now - entry.value > 30_000L) {
                        iterator.remove()
                    }
                }

                for (msgContent in messagesToProcess) {
                    // Filter system pings and summaries
                    if (msgContent.contains("Checking for new messages", ignoreCase = true) ||
                        msgContent.matches(Regex("\\d+\\s+new\\s+messages?", RegexOption.IGNORE_CASE))
                    ) {
                        continue
                    }

                    if (isDeletionNotice(msgContent)) {
                        val lookbackWindow = timestamp - (48 * 3600 * 1000L)
                        val marked = repository.markDeletedSmart(displaySender, cleanSender, lookbackWindow)
                        if (marked) {
                            notifyUserOfRecoveredDeletedMessage(cleanSender.ifBlank { displaySender })
                        }
                        continue
                    }

                    // Sliding window in-memory duplicate check
                    val sig = "$cleanSender|$msgContent"
                    val lastSeen = recentProcessedSignatures[sig]
                    if (lastSeen != null && (now - lastSeen < 25_000L)) {
                        // Skip duplicate notification repeated by WhatsApp within 25 seconds
                        continue
                    }
                    recentProcessedSignatures[sig] = now

                    // Securely store encrypted notification in Room database with DB-level deduplication
                    repository.storeVaultNotificationIfNotDuplicate(
                        NotificationVaultEntity(
                            packageName = pkg,
                            senderTitle = displaySender,
                            messageContent = msgContent,
                            timestamp = timestamp,
                            isDeletedDetected = false,
                            chatKey = cleanSender,
                            direction = "INCOMING"
                        )
                    )
                }

                // Automatic retention cleanup
                val retentionDays = preferencesRepository.autoDeleteDays.first()
                repository.pruneOldVaultRecords(retentionDays)
            } catch (e: Exception) {
                // Safeguard against background exceptions
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    private fun notifyUserOfRecoveredDeletedMessage(sender: String) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channelId = "wa_deleted_recovery_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Deleted Message Recovery Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when a deleted WhatsApp message is recovered in the vault"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }

            val appIntent = Intent(this, com.example.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                9999,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("WhatsApp Message Revoked!")
                .setContentText("A message from $sender was deleted, but safely preserved in your Vault.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("A message from $sender was deleted for everyone in WhatsApp, but has been safely recovered and encrypted in your Vault."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            manager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
        } catch (e: Exception) {
            // Silently ignore if permissions restricted
        }
    }

    companion object {
        fun normalizeSender(sender: String): String {
            return sender
                .replace(Regex("\\s*\\(\\d+\\s+(?:new\\s+)?messages?\\)", RegexOption.IGNORE_CASE), "")
                .replace(Regex("^WhatsApp:\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
        }

        fun isDeletionNotice(text: String): Boolean {
            val lower = text.lowercase().trim()
            return lower.contains("this message was deleted") ||
                    lower.contains("message was deleted") ||
                    lower.contains("message deleted") ||
                    lower.contains("you deleted this message") ||
                    lower.contains("sender revoked") ||
                    lower.contains("revoked a message") ||
                    lower.contains("revoked this message") ||
                    lower.contains("este mensaje fue eliminado") ||
                    lower.contains("ce message a été supprimé") ||
                    lower.contains("diese nachricht wurde gelöscht") ||
                    lower.contains("esta mensagem foi apagada") ||
                    lower.contains("questo messaggio è stato eliminato") ||
                    lower.contains("यह संदेश हटा दिया गया") ||
                    lower == "deleted" ||
                    lower == "revoked"
        }

        fun isPermissionGranted(context: Context): Boolean {
            val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
            return enabledListeners.contains(context.packageName)
        }

        fun openPermissionSettings(context: Context) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                    putExtra(
                        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        ComponentName(context, NotificationVaultService::class.java).flattenToString()
                    )
                }
            } else {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to standard notification listener settings
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
        }
    }
}
