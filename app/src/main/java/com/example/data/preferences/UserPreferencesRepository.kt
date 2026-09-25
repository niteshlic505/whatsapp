package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wa_pro_settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val VAULT_PIN_HASH = stringPreferencesKey("vault_pin_hash")
        val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
        val AUTO_DELETE_DAYS = intPreferencesKey("auto_delete_days")
        val DAILY_SAFETY_LIMIT = intPreferencesKey("daily_safety_limit")
        val DELAY_BETWEEN_MESSAGES_SEC = intPreferencesKey("delay_between_messages_sec")
        val HIGH_THINKING_ENABLED = booleanPreferencesKey("high_thinking_enabled")
        val EXCLUDED_CHATS = stringSetPreferencesKey("excluded_chats")
        val TODAY_SENT_COUNT = intPreferencesKey("today_sent_count")
        val LAST_SENT_DATE = stringPreferencesKey("last_sent_date")
        val SAFEGUARDS_ACTIVE = booleanPreferencesKey("safeguards_active")
    }

    val isPremium: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_PREMIUM] ?: false
    }

    val vaultPinHash: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.VAULT_PIN_HASH]
    }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_BIOMETRIC_ENABLED] ?: false
    }

    val autoDeleteDays: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_DELETE_DAYS] ?: 14 // default 14 days
    }

    val dailySafetyLimit: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DAILY_SAFETY_LIMIT] ?: 50 // 50 safe cap
    }

    val delayBetweenMessagesSec: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DELAY_BETWEEN_MESSAGES_SEC] ?: 8 // default 8 sec delay
    }

    val highThinkingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HIGH_THINKING_ENABLED] ?: true
    }

    val excludedChats: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.EXCLUDED_CHATS] ?: emptySet()
    }

    val todaySentCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TODAY_SENT_COUNT] ?: 3
    }

    val safeguardsActive: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SAFEGUARDS_ACTIVE] ?: true
    }

    suspend fun setPremium(isPremium: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_PREMIUM] = isPremium
        }
    }

    suspend fun setVaultPin(pinHash: String?) {
        context.dataStore.edit { preferences ->
            if (pinHash == null) {
                preferences.remove(PreferencesKeys.VAULT_PIN_HASH)
            } else {
                preferences[PreferencesKeys.VAULT_PIN_HASH] = pinHash
            }
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setAutoDeleteDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_DELETE_DAYS] = days
        }
    }

    suspend fun setDailySafetyLimit(limit: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_SAFETY_LIMIT] = limit
        }
    }

    suspend fun setDelayBetweenMessages(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DELAY_BETWEEN_MESSAGES_SEC] = seconds
        }
    }

    suspend fun setHighThinkingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIGH_THINKING_ENABLED] = enabled
        }
    }

    suspend fun setSafeguardsActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAFEGUARDS_ACTIVE] = active
        }
    }

    suspend fun addExcludedChat(chatName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.EXCLUDED_CHATS] ?: emptySet()
            preferences[PreferencesKeys.EXCLUDED_CHATS] = current + chatName
        }
    }

    suspend fun removeExcludedChat(chatName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.EXCLUDED_CHATS] ?: emptySet()
            preferences[PreferencesKeys.EXCLUDED_CHATS] = current - chatName
        }
    }

    suspend fun incrementTodaySentCount() {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.TODAY_SENT_COUNT] ?: 0
            preferences[PreferencesKeys.TODAY_SENT_COUNT] = current + 1
        }
    }

    suspend fun resetTodaySentCount() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TODAY_SENT_COUNT] = 0
        }
    }
}
