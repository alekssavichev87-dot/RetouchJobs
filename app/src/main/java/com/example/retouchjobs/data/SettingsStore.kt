package com.example.retouchjobs.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

const val DEFAULT_BLOCKED_DOMAINS =
    "avito.ru, hh.ru, hh.kz, superjob.ru, rabota.ru, zarplata.ru, job.ru, trudvsem.ru, youla.ru"

data class Settings(
    val apiKey: String = "",
    val blockedDomains: String = DEFAULT_BLOCKED_DOMAINS,
    val period: Period = Period.MONTH,
    val onlyVacancies: Boolean = true,
    val remote: Boolean = false,
    val autoContacts: Boolean = true,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val apiKey = stringPreferencesKey("api_key")
        val blocked = stringPreferencesKey("blocked_domains")
        val period = stringPreferencesKey("period")
        val onlyVacancies = booleanPreferencesKey("only_vacancies")
        val remote = booleanPreferencesKey("remote")
        val autoContacts = booleanPreferencesKey("auto_contacts")
    }

    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            apiKey = p[Keys.apiKey].orEmpty(),
            blockedDomains = p[Keys.blocked] ?: DEFAULT_BLOCKED_DOMAINS,
            period = p[Keys.period]
                ?.let { runCatching { Period.valueOf(it) }.getOrNull() }
                ?: Period.MONTH,
            onlyVacancies = p[Keys.onlyVacancies] ?: true,
            remote = p[Keys.remote] ?: false,
            autoContacts = p[Keys.autoContacts] ?: true,
        )
    }

    suspend fun saveSettings(apiKey: String, blocked: String, autoContacts: Boolean) {
        store.edit {
            it[Keys.apiKey] = apiKey.trim()
            it[Keys.blocked] = blocked.trim()
            it[Keys.autoContacts] = autoContacts
        }
    }

    suspend fun setPeriod(period: Period) {
        store.edit { it[Keys.period] = period.name }
    }

    suspend fun setOnlyVacancies(value: Boolean) {
        store.edit { it[Keys.onlyVacancies] = value }
    }

    suspend fun setRemote(value: Boolean) {
        store.edit { it[Keys.remote] = value }
    }
}
