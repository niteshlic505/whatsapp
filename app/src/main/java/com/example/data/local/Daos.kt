package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledMessageDao {
    @Query("SELECT * FROM scheduled_messages ORDER BY scheduledTimestamp ASC")
    fun getAllMessages(): Flow<List<ScheduledMessageEntity>>

    @Query("SELECT * FROM scheduled_messages WHERE status = :status ORDER BY scheduledTimestamp ASC")
    fun getMessagesByStatus(status: String): Flow<List<ScheduledMessageEntity>>

    @Query("SELECT * FROM scheduled_messages WHERE scheduledTimestamp <= :currentTime AND status = 'PENDING'")
    suspend fun getDuePendingMessages(currentTime: Long): List<ScheduledMessageEntity>

    @Query("SELECT COUNT(*) FROM scheduled_messages WHERE status = 'SENT' AND lastAttemptTimestamp >= :sinceTime")
    suspend fun getSentCountSince(sinceTime: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ScheduledMessageEntity): Long

    @Update
    suspend fun updateMessage(message: ScheduledMessageEntity)

    @Query("UPDATE scheduled_messages SET status = :status, lastAttemptTimestamp = :timestamp, errorMessage = :error WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, timestamp: Long, error: String? = null)

    @Delete
    suspend fun deleteMessage(message: ScheduledMessageEntity)

    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)
}

@Dao
interface MessageTemplateDao {
    @Query("SELECT * FROM message_templates ORDER BY isFavorite DESC, usageCount DESC, createdAt DESC")
    fun getAllTemplates(): Flow<List<MessageTemplateEntity>>

    @Query("SELECT * FROM message_templates WHERE category = :category ORDER BY isFavorite DESC, usageCount DESC")
    fun getTemplatesByCategory(category: String): Flow<List<MessageTemplateEntity>>

    @Query("SELECT * FROM message_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): MessageTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MessageTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: MessageTemplateEntity)

    @Query("UPDATE message_templates SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Long)

    @Query("UPDATE message_templates SET isFavorite = :isFav WHERE id = :id")
    suspend fun setFavorite(id: Long, isFav: Boolean)

    @Query("SELECT COUNT(*) FROM message_templates")
    suspend fun getTemplateCount(): Int

    @Delete
    suspend fun deleteTemplate(template: MessageTemplateEntity)
}

@Dao
interface NotificationVaultDao {
    @Query("SELECT * FROM notification_vault WHERE isExcluded = 0 ORDER BY timestamp DESC")
    fun getAllVaultNotifications(): Flow<List<NotificationVaultEntity>>

    @Query("SELECT * FROM notification_vault WHERE isDeletedDetected = 1 AND isExcluded = 0 ORDER BY timestamp DESC")
    fun getDeletedDetectedNotifications(): Flow<List<NotificationVaultEntity>>

    @Query("SELECT * FROM notification_vault WHERE direction = :direction AND isExcluded = 0 ORDER BY timestamp DESC")
    fun getVaultByDirection(direction: String): Flow<List<NotificationVaultEntity>>

    @Query("SELECT COUNT(*) FROM notification_vault WHERE isExcluded = 0")
    fun getVaultCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notification_vault WHERE isDeletedDetected = 1 AND isExcluded = 0")
    fun getDeletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notification_vault WHERE direction = 'INCOMING' AND isExcluded = 0")
    fun getIncomingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notification_vault WHERE direction = 'OUTGOING' AND isExcluded = 0")
    fun getOutgoingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(vaultItem: NotificationVaultEntity): Long

    @Query("SELECT * FROM notification_vault WHERE (chatKey = :chatKey OR senderTitle LIKE '%' || :cleanSender || '%' OR :cleanSender LIKE '%' || senderTitle || '%') ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentBySender(chatKey: String, cleanSender: String, limit: Int = 10): List<NotificationVaultEntity>

    @Query("UPDATE notification_vault SET isDeletedDetected = 1 WHERE id = :id")
    suspend fun markDeletedById(id: Long)

    @Query("UPDATE notification_vault SET isDeletedDetected = 1 WHERE (chatKey = :chatKey OR senderTitle LIKE '%' || :cleanSender || '%' OR :cleanSender LIKE '%' || senderTitle || '%') AND timestamp >= :afterTimestamp")
    suspend fun markDeletedFuzzy(chatKey: String, cleanSender: String, afterTimestamp: Long): Int

    @Query("UPDATE notification_vault SET isDeletedDetected = 1 WHERE chatKey = :chatKey AND timestamp >= :afterTimestamp")
    suspend fun markDeletedInChat(chatKey: String, afterTimestamp: Long)

    @Query("SELECT COUNT(*) FROM notification_vault WHERE (chatKey = :chatKey OR senderTitle = :sender) AND messageContent = :encryptedContent AND timestamp >= :sinceTimestamp")
    suspend fun countRecentDuplicate(chatKey: String, sender: String, encryptedContent: String, sinceTimestamp: Long): Int

    @Query("SELECT * FROM notification_vault WHERE (chatKey = :sender OR senderTitle LIKE '%' || :sender || '%') ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentChatHistory(sender: String, limit: Int = 20): List<NotificationVaultEntity>

    @Query("DELETE FROM notification_vault WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM notification_vault WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notification_vault")
    suspend fun clearAll()
}

@Dao
interface AutomationWorkflowDao {
    @Query("SELECT * FROM automation_workflows ORDER BY createdAt DESC")
    fun getAllWorkflows(): Flow<List<AutomationWorkflowEntity>>

    @Query("SELECT * FROM automation_workflows WHERE isEnabled = 1")
    suspend fun getActiveWorkflows(): List<AutomationWorkflowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: AutomationWorkflowEntity): Long

    @Update
    suspend fun updateWorkflow(workflow: AutomationWorkflowEntity)

    @Query("UPDATE automation_workflows SET isEnabled = :enabled WHERE id = :id")
    suspend fun setWorkflowEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE automation_workflows SET runCount = runCount + 1, lastRunTime = :runTime WHERE id = :id")
    suspend fun recordWorkflowRun(id: Long, runTime: Long)

    @Delete
    suspend fun deleteWorkflow(workflow: AutomationWorkflowEntity)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE tag = :tag ORDER BY name ASC")
    fun getContactsByTag(tag: String): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)
}

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns ORDER BY createdAt DESC")
    fun getAllCampaigns(): Flow<List<CampaignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: CampaignEntity): Long

    @Update
    suspend fun updateCampaign(campaign: CampaignEntity)

    @Query("UPDATE campaigns SET sentRecipients = :sentCount, status = :status WHERE id = :id")
    suspend fun updateProgress(id: Long, sentCount: Int, status: String)

    @Delete
    suspend fun deleteCampaign(campaign: CampaignEntity)
}
