package com.example

import android.app.Application
import com.example.data.ai.GeminiAiService
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AppRepository
import com.example.data.security.VaultSecurityManager
import com.example.engine.ScheduledMessageWorker
import com.example.engine.WhatsAppDispatcher

class WAApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var securityManager: VaultSecurityManager
        private set

    lateinit var repository: AppRepository
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var aiService: GeminiAiService
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            database = AppDatabase.getInstance(this)
            securityManager = VaultSecurityManager(this)
            repository = AppRepository(this, database, securityManager)
            preferencesRepository = UserPreferencesRepository(this)
            aiService = GeminiAiService()

            WhatsAppDispatcher.createNotificationChannels(this)
            ScheduledMessageWorker.enqueuePeriodicCheck(this)
        } catch (e: Throwable) {
            android.util.Log.e("WAApplication", "Initialization error caught safely", e)
        }
    }

    companion object {
        lateinit var instance: WAApplication
            private set
    }
}
