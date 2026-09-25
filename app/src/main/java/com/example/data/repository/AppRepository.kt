package com.example.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.security.VaultSecurityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AppRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val securityManager: VaultSecurityManager
) {

    private val scheduledMessageDao = database.scheduledMessageDao()
    private val templateDao = database.messageTemplateDao()
    private val vaultDao = database.notificationVaultDao()
    private val workflowDao = database.automationWorkflowDao()
    private val contactDao = database.contactDao()
    private val campaignDao = database.campaignDao()

    init {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.populateInitialDataIfEmpty(database)
            } catch (e: Exception) {
                // Ignore initialization failures
            }
        }
    }

    // --- Messages ---
    val allScheduledMessages: Flow<List<ScheduledMessageEntity>> = scheduledMessageDao.getAllMessages()

    suspend fun scheduleMessage(message: ScheduledMessageEntity): Long {
        return scheduledMessageDao.insertMessage(message)
    }

    suspend fun updateMessageStatus(id: Long, status: String, error: String? = null) {
        scheduledMessageDao.updateStatus(id, status, System.currentTimeMillis(), error)
    }

    suspend fun deleteMessage(id: Long) {
        scheduledMessageDao.deleteMessageById(id)
    }

    suspend fun getDueMessages(): List<ScheduledMessageEntity> {
        return scheduledMessageDao.getDuePendingMessages(System.currentTimeMillis())
    }

    // --- Templates ---
    val allTemplates: Flow<List<MessageTemplateEntity>> = templateDao.getAllTemplates()

    fun getTemplatesByCategory(category: String): Flow<List<MessageTemplateEntity>> {
        return if (category == "All") {
            templateDao.getAllTemplates()
        } else {
            templateDao.getTemplatesByCategory(category)
        }
    }

    suspend fun saveTemplate(template: MessageTemplateEntity): Long {
        return templateDao.insertTemplate(template)
    }

    suspend fun toggleTemplateFavorite(id: Long, currentFavorite: Boolean) {
        templateDao.setFavorite(id, !currentFavorite)
    }

    suspend fun incrementTemplateUsage(id: Long) {
        templateDao.incrementUsage(id)
    }

    suspend fun deleteTemplate(template: MessageTemplateEntity) {
        templateDao.deleteTemplate(template)
    }

    // --- Notification Vault (Encrypted locally) ---
    val allVaultNotifications: Flow<List<NotificationVaultEntity>> = vaultDao.getAllVaultNotifications()
        .map { list ->
            list.map { item ->
                item.copy(messageContent = securityManager.decrypt(item.messageContent))
            }
        }

    val deletedDetectedNotifications: Flow<List<NotificationVaultEntity>> = vaultDao.getDeletedDetectedNotifications()
        .map { list ->
            list.map { item ->
                item.copy(messageContent = securityManager.decrypt(item.messageContent))
            }
        }

    val vaultCount: Flow<Int> = vaultDao.getVaultCount()
    val deletedVaultCount: Flow<Int> = vaultDao.getDeletedCount()
    val incomingVaultCount: Flow<Int> = vaultDao.getIncomingCount()
    val outgoingVaultCount: Flow<Int> = vaultDao.getOutgoingCount()

    fun getVaultByDirection(direction: String): Flow<List<NotificationVaultEntity>> {
        return vaultDao.getVaultByDirection(direction).map { list ->
            list.map { item ->
                item.copy(messageContent = securityManager.decrypt(item.messageContent))
            }
        }
    }

    suspend fun storeVaultNotification(item: NotificationVaultEntity): Long {
        val encryptedContent = securityManager.encrypt(item.messageContent)
        return vaultDao.insertNotification(item.copy(messageContent = encryptedContent))
    }

    suspend fun recordOutgoingMessage(
        recipientName: String,
        recipientPhone: String,
        messageText: String
    ): Long {
        val displaySender = if (recipientName.isNotBlank() && recipientPhone.isNotBlank() && recipientName != recipientPhone) {
            "You ➔ $recipientName ($recipientPhone)"
        } else if (recipientName.isNotBlank()) {
            "You ➔ $recipientName"
        } else if (recipientPhone.isNotBlank()) {
            "You ➔ $recipientPhone"
        } else {
            "You ➔ WhatsApp Recipient"
        }

        val item = NotificationVaultEntity(
            packageName = "com.whatsapp",
            senderTitle = displaySender,
            messageContent = messageText,
            timestamp = System.currentTimeMillis(),
            isDeletedDetected = false,
            chatKey = recipientPhone.ifBlank { recipientName },
            direction = "OUTGOING"
        )
        return storeVaultNotification(item)
    }

    suspend fun storeVaultNotificationIfNotDuplicate(item: NotificationVaultEntity): Boolean {
        val encryptedContent = securityManager.encrypt(item.messageContent)
        // Deduplicate against messages stored within recent 10 minutes
        val recentThreshold = item.timestamp - (10 * 60 * 1000L)
        val dupes = vaultDao.countRecentDuplicate(item.chatKey, item.senderTitle, encryptedContent, recentThreshold)
        if (dupes > 0) {
            return false // Duplicate blocked
        }
        vaultDao.insertNotification(item.copy(messageContent = encryptedContent))
        return true
    }

    suspend fun markDeletedSmart(chatKey: String, cleanSender: String, afterTimestamp: Long): Boolean {
        // Step 1: Attempt fuzzy match within time window
        val count = vaultDao.markDeletedFuzzy(chatKey, cleanSender, afterTimestamp)
        if (count > 0) return true

        // Step 2: Look back up to 48 hours for the most recent message from this sender
        val lookback = System.currentTimeMillis() - (48 * 3600 * 1000L)
        val recentList = vaultDao.getRecentBySender(chatKey, cleanSender, limit = 5)
        val candidate = recentList.firstOrNull { !it.isDeletedDetected && it.timestamp >= lookback }
        if (candidate != null) {
            vaultDao.markDeletedById(candidate.id)
            return true
        }

        // Step 3: If no prior message in vault exists (e.g. app installed after message or deleted immediately),
        // record the deletion notification explicitly so the user sees it in the vault!
        val notice = NotificationVaultEntity(
            packageName = "com.whatsapp",
            senderTitle = cleanSender.ifBlank { chatKey },
            messageContent = "This message was deleted by the sender.",
            timestamp = System.currentTimeMillis(),
            isDeletedDetected = true,
            chatKey = chatKey
        )
        storeVaultNotification(notice)
        return true
    }

    suspend fun markDeletedInChat(chatKey: String, afterTimestamp: Long) {
        markDeletedSmart(chatKey, chatKey, afterTimestamp)
    }

    suspend fun getDecryptedRecentChatHistory(sender: String, limit: Int = 20): List<NotificationVaultEntity> {
        val raw = vaultDao.getRecentChatHistory(sender, limit)
        return raw.map { item ->
            item.copy(messageContent = securityManager.decrypt(item.messageContent))
        }
    }

    suspend fun pruneOldVaultRecords(retentionDays: Int): Int {
        if (retentionDays <= 0) return 0
        val cutoff = System.currentTimeMillis() - (retentionDays * 86400_000L)
        return vaultDao.deleteOlderThan(cutoff)
    }

    suspend fun clearVault() {
        vaultDao.clearAll()
    }

    suspend fun deleteVaultNotification(id: Long) {
        vaultDao.deleteById(id)
    }

    // --- Automation Workflows ---
    val allWorkflows: Flow<List<AutomationWorkflowEntity>> = workflowDao.getAllWorkflows()

    suspend fun saveWorkflow(workflow: AutomationWorkflowEntity): Long {
        return workflowDao.insertWorkflow(workflow)
    }

    suspend fun setWorkflowEnabled(id: Long, enabled: Boolean) {
        workflowDao.setWorkflowEnabled(id, enabled)
    }

    suspend fun deleteWorkflow(workflow: AutomationWorkflowEntity) {
        workflowDao.deleteWorkflow(workflow)
    }

    suspend fun recordWorkflowRun(id: Long) {
        workflowDao.recordWorkflowRun(id, System.currentTimeMillis())
    }

    // --- Contacts ---
    val allContacts: Flow<List<ContactEntity>> = contactDao.getAllContacts()

    fun getContactsByTag(tag: String): Flow<List<ContactEntity>> {
        return if (tag == "All") contactDao.getAllContacts() else contactDao.getContactsByTag(tag)
    }

    suspend fun addContact(contact: ContactEntity): Long {
        return contactDao.insertContact(contact)
    }

    suspend fun addContacts(contacts: List<ContactEntity>) {
        contactDao.insertContacts(contacts)
    }

    suspend fun deleteContact(id: Long) {
        contactDao.deleteContactById(id)
    }

    suspend fun importDeviceContacts(): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val resolver = context.contentResolver
            val cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT 200"
            )

            val imported = mutableListOf<ContactEntity>()
            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val name = it.getString(nameIdx) ?: "Contact"
                    val number = it.getString(numberIdx)?.replace(" ", "")?.replace("-", "") ?: ""
                    if (number.isNotBlank()) {
                        imported.add(
                            ContactEntity(
                                name = name,
                                phoneNumber = number,
                                tag = "Customers"
                            )
                        )
                    }
                }
            }
            if (imported.isNotEmpty()) {
                contactDao.insertContacts(imported)
                count = imported.size
            }
        } catch (e: Exception) {
            // Permission or security exception
        }
        count
    }

    // --- Campaigns ---
    val allCampaigns: Flow<List<CampaignEntity>> = campaignDao.getAllCampaigns()

    suspend fun createCampaign(campaign: CampaignEntity): Long {
        return campaignDao.insertCampaign(campaign)
    }

    suspend fun updateCampaignProgress(id: Long, sent: Int, status: String) {
        campaignDao.updateProgress(id, sent, status)
    }

    suspend fun deleteCampaign(campaign: CampaignEntity) {
        campaignDao.deleteCampaign(campaign)
    }

    // --- Variable Personalization Helper ---
    fun personalizeMessage(
        rawTemplate: String,
        contactName: String,
        extraVars: Map<String, String> = emptyMap()
    ): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val now = Date()

        val firstName = contactName.split(" ").firstOrNull() ?: contactName
        var result = rawTemplate
            .replace("{name}", contactName)
            .replace("{first_name}", firstName)
            .replace("{date}", dateFormat.format(now))
            .replace("{time}", timeFormat.format(now))

        for ((key, value) in extraVars) {
            result = result.replace("{$key}", value)
        }
        return result
    }

    // --- Duplicate & Spam Risk Detector ---
    data class SpamRiskAnalysis(
        val riskScore: Int, // 0 - 100
        val riskLevel: String, // "Low", "Moderate", "High", "Critical"
        val flags: List<String>
    )

    fun analyzeSpamRisk(messageText: String, isBulk: Boolean = false): SpamRiskAnalysis {
        val flags = mutableListOf<String>()
        var score = 0

        // Uppercase ratio check
        val letters = messageText.filter { it.isLetter() }
        if (letters.isNotEmpty()) {
            val upperCount = letters.count { it.isUpperCase() }
            val ratio = upperCount.toFloat() / letters.length
            if (ratio > 0.4f) {
                score += 30
                flags.add("High UPPERCASE character ratio ($ ${(ratio * 100).toInt()}%)")
            }
        }

        // Spam keywords
        val spamWords = listOf("FREE", "100%", "URGENT", "ACT NOW", "GUARANTEED", "WINNER", "LOTTERY", "CLICK HERE", "BANK", "PASSWORD")
        val foundSpamWords = spamWords.filter { messageText.uppercase().contains(it) }
        if (foundSpamWords.isNotEmpty()) {
            score += foundSpamWords.size * 15
            flags.add("Spam trigger words detected: ${foundSpamWords.joinToString()}")
        }

        // Excessive exclamation marks or links
        val exclamations = messageText.count { it == '!' }
        if (exclamations > 3) {
            score += 15
            flags.add("Multiple consecutive exclamation marks ($exclamations)")
        }

        val urlCount = "http".toRegex().findAll(messageText.lowercase()).count()
        if (urlCount > 2) {
            score += 20
            flags.add("Multiple hyperlinks ($urlCount)")
        }

        if (isBulk) {
            score += 10
            flags.add("Bulk campaign dispatch safeguard applied")
        }

        val finalScore = score.coerceIn(0, 100)
        val level = when {
            finalScore < 25 -> "Safe"
            finalScore < 55 -> "Moderate"
            finalScore < 80 -> "High Risk"
            else -> "Critical Risk"
        }
        return SpamRiskAnalysis(finalScore, level, flags)
    }
}
