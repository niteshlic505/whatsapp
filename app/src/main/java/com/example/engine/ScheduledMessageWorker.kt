package com.example.engine

import android.content.Context
import androidx.work.*
import com.example.data.local.AppDatabase
import com.example.data.model.ScheduledMessageEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AppRepository
import com.example.data.security.VaultSecurityManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ScheduledMessageWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val sec = VaultSecurityManager(applicationContext)
        val repo = AppRepository(applicationContext, db, sec)
        val prefs = UserPreferencesRepository(applicationContext)

        val dueMessages = repo.getDueMessages()
        val dailyLimit = prefs.dailySafetyLimit.first()
        val sentToday = prefs.todaySentCount.first()

        for (msg in dueMessages) {
            if (sentToday >= dailyLimit) {
                // Exceeded daily safety cap
                repo.updateMessageStatus(msg.id, "FAILED", "Paused by Safety Cap ($dailyLimit msgs/day reached)")
                continue
            }

            // Fire heads-up action alert so user or auto-helper can open WhatsApp
            WhatsAppDispatcher.showScheduledDispatchAlert(
                applicationContext,
                msg.id,
                msg.recipientName,
                msg.recipient,
                msg.messageText
            )

            // Handle recurrence
            when (msg.recurrence) {
                "DAILY" -> {
                    val nextTime = msg.scheduledTimestamp + TimeUnit.DAYS.toMillis(1)
                    repo.scheduleMessage(msg.copy(id = 0, scheduledTimestamp = nextTime, status = "PENDING"))
                    repo.updateMessageStatus(msg.id, "SENT")
                }
                "WEEKLY" -> {
                    val nextTime = msg.scheduledTimestamp + TimeUnit.DAYS.toMillis(7)
                    repo.scheduleMessage(msg.copy(id = 0, scheduledTimestamp = nextTime, status = "PENDING"))
                    repo.updateMessageStatus(msg.id, "SENT")
                }
                "MONTHLY" -> {
                    val nextTime = msg.scheduledTimestamp + TimeUnit.DAYS.toMillis(30)
                    repo.scheduleMessage(msg.copy(id = 0, scheduledTimestamp = nextTime, status = "PENDING"))
                    repo.updateMessageStatus(msg.id, "SENT")
                }
                else -> {
                    repo.updateMessageStatus(msg.id, "SENT")
                }
            }

            prefs.incrementTodaySentCount()
        }

        return Result.success()
    }

    companion object {
        fun enqueuePeriodicCheck(context: Context) {
            try {
                val request = PeriodicWorkRequestBuilder<ScheduledMessageWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiresBatteryNotLow(false)
                            .build()
                    )
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "WAAutomationPeriodicWorker",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (e: Exception) {
                // Graceful fallback in environments without initialized WorkManager
            }
        }

        fun enqueueOneTimeCheck(context: Context, delayMillis: Long = 0) {
            try {
                val builder = OneTimeWorkRequestBuilder<ScheduledMessageWorker>()
                if (delayMillis > 0) {
                    builder.setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                }
                WorkManager.getInstance(context).enqueue(builder.build())
            } catch (e: Exception) {
                // Graceful fallback
            }
        }
    }
}
