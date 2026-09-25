package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ScheduledMessageEntity::class,
        MessageTemplateEntity::class,
        NotificationVaultEntity::class,
        AutomationWorkflowEntity::class,
        ContactEntity::class,
        CampaignEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun messageTemplateDao(): MessageTemplateDao
    abstract fun notificationVaultDao(): NotificationVaultDao
    abstract fun automationWorkflowDao(): AutomationWorkflowDao
    abstract fun contactDao(): ContactDao
    abstract fun campaignDao(): CampaignDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wa_automation_pro.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateInitialDataIfEmpty(db: AppDatabase) {
            try {
                val templateDao = db.messageTemplateDao()
                if (templateDao.getTemplateCount() > 0) return
                populateDefaultData(db)
            } catch (e: Exception) {
                // Safeguard against DB conflicts
            }
        }

        private suspend fun populateDefaultData(db: AppDatabase) {
            val templateDao = db.messageTemplateDao()
            val defaultTemplates = listOf(
                MessageTemplateEntity(
                    title = "Business Meeting Reminder",
                    content = "Hi {name}, friendly reminder about our scheduled meeting on {date} at {time}. Please let me know if you need to adjust!",
                    category = "Business",
                    isFavorite = true,
                    usageCount = 12
                ),
                MessageTemplateEntity(
                    title = "Lead Follow-up Sequence #1",
                    content = "Hello {name}, thank you for your interest in our services! Here is our quick overview. Would tomorrow at 10 AM suit you for a quick 5-min chat?",
                    category = "Follow-up",
                    isFavorite = true,
                    usageCount = 28
                ),
                MessageTemplateEntity(
                    title = "Diwali & Festive Prosperity Greeting",
                    content = "Namaste {name}! 🪔 Wishing you and your family a very Happy and Prosperous Diwali! May the divine lights bring immense joy, good health, and boundless success to your home and business.",
                    category = "Greetings",
                    isFavorite = true,
                    usageCount = 42
                ),
                MessageTemplateEntity(
                    title = "UPI Payment & Invoice Confirmation",
                    content = "Namaste {name}, we have received your payment successfully via UPI / Net Banking. Thank you for doing business with us! Your tax invoice has been generated.",
                    category = "Business",
                    isFavorite = true,
                    usageCount = 35
                ),
                MessageTemplateEntity(
                    title = "GST Invoice & Delivery Dispatch",
                    content = "Dear {name}, your order has been dispatched along with the GST Tax Invoice. You can track delivery status directly on this WhatsApp chat. Have a wonderful day!",
                    category = "Business",
                    isFavorite = false,
                    usageCount = 20
                ),
                MessageTemplateEntity(
                    title = "Bharat Independence & Republic Day",
                    content = "Happy Independence Day, {name}! 🇮🇳 Celebrating freedom, unity, and the spirit of a rising Bharat with pride and heartfelt greetings to you and your loved ones.",
                    category = "Greetings",
                    isFavorite = true,
                    usageCount = 30
                ),
                MessageTemplateEntity(
                    title = "Gentle Business Follow-up",
                    content = "Namaste {name}, hope you are having a productive week! Following up regarding our proposal. Please let me know a convenient time for a quick WhatsApp call.",
                    category = "Personal",
                    isFavorite = false,
                    usageCount = 14
                )
            )
            for (t in defaultTemplates) {
                templateDao.insertTemplate(t)
            }

            val contactDao = db.contactDao()
            val defaultContacts = listOf(
                ContactEntity(name = "Rahul Sharma", phoneNumber = "+91 98201 55012", tag = "VIP", notes = "Enterprise Partner, Mumbai (BKC)"),
                ContactEntity(name = "Priya Patel", phoneNumber = "+91 98795 44210", tag = "Customers", notes = "Textiles Bulk Buyer, Ahmedabad"),
                ContactEntity(name = "Vikram Singhania", phoneNumber = "+91 98100 88231", tag = "VIP", notes = "Managing Director, Delhi NCR"),
                ContactEntity(name = "Ananya Deshmukh", phoneNumber = "+91 99220 33145", tag = "Leads", notes = "SaaS Automations, Pune"),
                ContactEntity(name = "Dr. Rajeshwar Varma", phoneNumber = "+91 98450 67123", tag = "Customers", notes = "Bengaluru Semiconductor Hub")
            )
            contactDao.insertContacts(defaultContacts)

            val workflowDao = db.automationWorkflowDao()
            val defaultWorkflows = listOf(
                AutomationWorkflowEntity(
                    name = "Daily Morning Briefing Reminder",
                    description = "Sends WhatsApp agenda to team leads every morning at 09:00 AM",
                    triggerType = "TIME_SCHEDULE",
                    triggerValue = "09:00 AM",
                    conditionText = "Weekdays only & Safe limit check",
                    actionType = "SEND_MESSAGE",
                    actionPayload = "Good morning {name}, here is today's scheduled roadmap!",
                    delayMinutes = 0,
                    isEnabled = true,
                    runCount = 14
                ),
                AutomationWorkflowEntity(
                    name = "New Lead 24h Auto Follow-up",
                    description = "When a contact is tagged as 'Leads', prepare a follow-up 24 hours later",
                    triggerType = "CONTACT_TAG",
                    triggerValue = "Tag: Leads",
                    conditionText = "Unanswered after 24 hours",
                    actionType = "SEND_MESSAGE",
                    actionPayload = "Hi {name}, following up to see if you had any questions regarding yesterday's preview!",
                    delayMinutes = 1440,
                    isEnabled = true,
                    runCount = 8
                ),
                AutomationWorkflowEntity(
                    name = "Weekly Monday Status Ping",
                    description = "Recurring Monday greeting and check-in to active clients",
                    triggerType = "RECURRING_INTERVAL",
                    triggerValue = "Weekly on Monday",
                    conditionText = "Tag: Customers & Safe delay active",
                    actionType = "SEND_MESSAGE",
                    actionPayload = "Wishing you a productive and fruitful week, {name}!",
                    delayMinutes = 0,
                    isEnabled = false,
                    runCount = 3
                )
            )
            for (w in defaultWorkflows) {
                workflowDao.insertWorkflow(w)
            }

            val scheduledDao = db.scheduledMessageDao()
            val now = System.currentTimeMillis()
            scheduledDao.insertMessage(
                ScheduledMessageEntity(
                    recipient = "+1 555-0192",
                    recipientName = "Sarah Jenkins",
                    messageText = "Hi Sarah, confirming our roadmap review on Friday at 3 PM.",
                    scheduledTimestamp = now + 3600_000L * 2, // 2 hours ahead
                    recurrence = "NONE",
                    status = "PENDING"
                )
            )
            scheduledDao.insertMessage(
                ScheduledMessageEntity(
                    recipient = "+1 555-0371",
                    recipientName = "Elena Rostova",
                    messageText = "Hello Elena, our product demo has been arranged for tomorrow!",
                    scheduledTimestamp = now + 3600_000L * 5,
                    recurrence = "DAILY",
                    status = "PENDING"
                )
            )
            scheduledDao.insertMessage(
                ScheduledMessageEntity(
                    recipient = "+1 555-0284",
                    recipientName = "David Chen",
                    messageText = "Hi David, your monthly analytics summary is ready for download.",
                    scheduledTimestamp = now - 3600_000L * 24,
                    recurrence = "NONE",
                    status = "SENT",
                    lastAttemptTimestamp = now - 3600_000L * 24
                )
            )

            val vaultDao = db.notificationVaultDao()
            vaultDao.insertNotification(
                NotificationVaultEntity(
                    packageName = "com.whatsapp",
                    senderTitle = "Sarah Jenkins",
                    messageContent = "Can we reschedule our 3 PM call by 30 minutes?",
                    timestamp = now - 1800_000L,
                    isDeletedDetected = false,
                    chatKey = "Sarah Jenkins"
                )
            )
            vaultDao.insertNotification(
                NotificationVaultEntity(
                    packageName = "com.whatsapp",
                    senderTitle = "Elena Rostova",
                    messageContent = "The proposal document has been updated with new rates.",
                    timestamp = now - 7200_000L,
                    isDeletedDetected = true,
                    chatKey = "Elena Rostova"
                )
            )
        }
    }
}
