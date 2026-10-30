package com.example.retouchjobs.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Properties

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

/**
 * Настройки лежат в файле %APPDATA%\RetouchJobs\settings.properties
 * (на других системах — в домашней папке пользователя). Только на этом компьютере.
 */
class SettingsStore {
    private val file: File = run {
        val base = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
            ?: System.getProperty("user.home")
        File(File(base, "RetouchJobs"), "settings.properties")
    }

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private fun load(): Settings {
        val p = Properties()
        runCatching {
            if (file.isFile) file.reader(Charsets.UTF_8).use { p.load(it) }
        }
        return Settings(
            apiKey = p.getProperty("api_key").orEmpty(),
            blockedDomains = p.getProperty("blocked_domains") ?: DEFAULT_BLOCKED_DOMAINS,
            period = p.getProperty("period")
                ?.let { runCatching { Period.valueOf(it) }.getOrNull() }
                ?: Period.MONTH,
            onlyVacancies = p.getProperty("only_vacancies")?.toBooleanStrictOrNull() ?: true,
            remote = p.getProperty("remote")?.toBooleanStrictOrNull() ?: false,
            autoContacts = p.getProperty("auto_contacts")?.toBooleanStrictOrNull() ?: true,
        )
    }

    private fun save(s: Settings) {
        val p = Properties().apply {
            setProperty("api_key", s.apiKey)
            setProperty("blocked_domains", s.blockedDomains)
            setProperty("period", s.period.name)
            setProperty("only_vacancies", s.onlyVacancies.toString())
            setProperty("remote", s.remote.toString())
            setProperty("auto_contacts", s.autoContacts.toString())
        }
        runCatching {
            file.parentFile?.mkdirs()
            file.writer(Charsets.UTF_8).use { p.store(it, "RetouchJobs settings") }
        }
    }

    private fun update(transform: (Settings) -> Settings) {
        val next = transform(_settings.value)
        _settings.value = next
        save(next)
    }

    fun saveSettings(apiKey: String, blocked: String, autoContacts: Boolean) = update {
        it.copy(apiKey = apiKey.trim(), blockedDomains = blocked.trim(), autoContacts = autoContacts)
    }

    fun setPeriod(period: Period) = update { it.copy(period = period) }

    fun setOnlyVacancies(value: Boolean) = update { it.copy(onlyVacancies = value) }

    fun setRemote(value: Boolean) = update { it.copy(remote = value) }
}
