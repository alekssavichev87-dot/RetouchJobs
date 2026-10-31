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

/** Папка данных приложения: %APPDATA%\RetouchJobs (на других системах — домашняя папка). */
object AppDirs {
    val dir: File by lazy {
        val base = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
            ?: System.getProperty("user.home")
        File(base, "RetouchJobs").also { it.mkdirs() }
    }
}

/**
 * Настройки лежат в %APPDATA%\RetouchJobs\settings.properties. API-ключ в этом файле
 * хранится только в зашифрованном виде (Windows DPAPI), открытым текстом его там нет.
 */
class SettingsStore {
    private val file = File(AppDirs.dir, "settings.properties")

    // Чтобы не запускать шифрование при каждом нажатии на фильтр, помним последний результат.
    private var cachedKey: String = ""
    private var cachedEnc: String? = null

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private fun load(): Settings {
        val p = Properties()
        runCatching {
            if (file.isFile) file.reader(Charsets.UTF_8).use { p.load(it) }
        }

        val enc = p.getProperty("api_key_dpapi")
        val legacy = p.getProperty("api_key").orEmpty()
        val key = enc?.let { WindowsDpapi.unprotect(it) } ?: legacy
        cachedKey = key
        cachedEnc = enc?.takeIf { key.isNotEmpty() && key != legacy }

        val settings = Settings(
            apiKey = key,
            blockedDomains = p.getProperty("blocked_domains") ?: DEFAULT_BLOCKED_DOMAINS,
            period = p.getProperty("period")
                ?.let { runCatching { Period.valueOf(it) }.getOrNull() }
                ?: Period.MONTH,
            onlyVacancies = p.getProperty("only_vacancies")?.toBooleanStrictOrNull() ?: true,
            remote = p.getProperty("remote")?.toBooleanStrictOrNull() ?: false,
            autoContacts = p.getProperty("auto_contacts")?.toBooleanStrictOrNull() ?: true,
        )

        // Миграция: ключ, сохранённый старой версией открытым текстом, шифруем и убираем из файла.
        if (legacy.isNotEmpty() && enc == null) save(settings)
        return settings
    }

    private fun save(s: Settings) {
        val encrypted = when {
            s.apiKey.isEmpty() -> null
            s.apiKey == cachedKey && cachedEnc != null -> cachedEnc
            else -> WindowsDpapi.protect(s.apiKey)
        }
        cachedKey = s.apiKey
        cachedEnc = encrypted

        val p = Properties().apply {
            when {
                s.apiKey.isEmpty() -> Unit
                encrypted != null -> setProperty("api_key_dpapi", encrypted)
                // Шифрование недоступно: лучше сохранить ключ, чем потерять.
                else -> setProperty("api_key", s.apiKey)
            }
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
