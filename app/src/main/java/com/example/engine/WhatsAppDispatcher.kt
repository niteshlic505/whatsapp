package com.example.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.net.URLEncoder

object WhatsAppDispatcher {

    const val CHANNEL_ID_AUTOMATION = "wa_automation_channel"
    const val CHANNEL_NAME_AUTOMATION = "WA Automation Alerts"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_AUTOMATION,
                CHANNEL_NAME_AUTOMATION,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts and reminders for scheduled WhatsApp automations"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun isWhatsAppInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: Exception) {
            try {
                pm.getPackageInfo("com.whatsapp.w4b", PackageManager.GET_ACTIVITIES)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    fun createWhatsAppIntent(recipientPhone: String, messageText: String): Intent {
        val cleanPhone = recipientPhone.replace("+", "")
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        val encodedMsg = try {
            URLEncoder.encode(messageText, "UTF-8")
        } catch (e: Exception) {
            Uri.encode(messageText)
        }

        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // Target package if present
        return intent
    }

    fun showScheduledDispatchAlert(
        context: Context,
        messageId: Long,
        recipientName: String,
        recipientPhone: String,
        messageText: String
    ) {
        createNotificationChannels(context)

        val waIntent = createWhatsAppIntent(recipientPhone, messageText)
        val sendPendingIntent = PendingIntent.getActivity(
            context,
            messageId.toInt(),
            waIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            (messageId + 1000).toInt(),
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_AUTOMATION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("WA Automation Due: $recipientName")
            .setContentText(messageText.take(90))
            .setStyle(NotificationCompat.BigTextStyle().bigText("Recipient: $recipientName ($recipientPhone)\n\n$messageText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_send,
                "Send via WhatsApp",
                sendPendingIntent
            )
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(messageId.toInt(), notification)
    }
}
