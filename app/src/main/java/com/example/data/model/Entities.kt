package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_messages")
data class ScheduledMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipient: String,
    val recipientName: String,
    val messageText: String,
    val scheduledTimestamp: Long,
    val recurrence: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY
    val status: String = "PENDING",   // PENDING, SENT, FAILED, CANCELLED
    val mediaUri: String? = null,
    val sequenceId: Long? = null,
    val sequenceStep: Int = 1,
    val lastAttemptTimestamp: Long? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "message_templates")
data class MessageTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String, // Business, Follow-up, Greetings, Support, Personal, Sales
    val isFavorite: Boolean = false,
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_vault")
data class NotificationVaultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val senderTitle: String,
    val messageContent: String,
    val timestamp: Long,
    val isDeletedDetected: Boolean = false,
    val chatKey: String,
    val isExcluded: Boolean = false,
    val direction: String = "INCOMING" // "INCOMING" or "OUTGOING"
)

@Entity(tableName = "automation_workflows")
data class AutomationWorkflowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val triggerType: String, // TIME_SCHEDULE, RECURRING_INTERVAL, CONTACT_TAG, FOLLOW_UP_CHAIN
    val triggerValue: String, // e.g., "09:00 AM", "Weekly Monday", "Tag: Leads"
    val conditionText: String, // e.g., "Safe Cap Valid & Weekdays"
    val actionType: String,   // "SEND_MESSAGE", "SEND_REMINDER", "APPLY_TAG"
    val actionPayload: String, // Template text or instruction
    val delayMinutes: Int = 0,
    val isEnabled: Boolean = true,
    val runCount: Int = 0,
    val lastRunTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val tag: String = "Customers", // Customers, Friends, Leads, Follow-up, VIP
    val notes: String = "",
    val source: String = "Manual", // Manual, PDF Import, Excel Import, Image OCR, AI Extracted
    val lastContacted: Long? = null
)

@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val templateText: String,
    val targetTag: String,
    val totalRecipients: Int,
    val sentRecipients: Int = 0,
    val delaySeconds: Int = 8,
    val status: String = "DRAFT", // DRAFT, RUNNING, COMPLETED, PAUSED
    val createdAt: Long = System.currentTimeMillis()
)
