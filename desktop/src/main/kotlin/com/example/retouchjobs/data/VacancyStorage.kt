package com.example.retouchjobs.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Что хранится на устройстве между запусками: последние результаты каждого раздела
 * (со ссылкой, описанием, контактами и итогом проверки) и дата, когда каждая вакансия
 * впервые попалась в поиске.
 */
@Serializable
data class StoredData(
    val firstSeen: Map<String, Long> = emptyMap(),
    val results: Map<String, List<Vacancy>> = emptyMap(),
)

/**
 * Простое файловое хранилище в JSON. Данных мало (десятки вакансий), поэтому база не нужна,
 * а один и тот же код работает и на Android, и на Windows без дополнительных библиотек.
 * Запись атомарная: сначала во временный файл, потом замена.
 */
class VacancyStorage(private val file: File) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun load(): StoredData = try {
        if (file.isFile) json.decodeFromString<StoredData>(file.readText(Charsets.UTF_8)) else StoredData()
    } catch (e: Exception) {
        StoredData() // повреждённый файл не должен ронять приложение
    }

    fun save(data: StoredData) {
        val dir = file.parentFile ?: return
        dir.mkdirs()
        val tmp = File(dir, file.name + ".tmp")
        tmp.writeText(json.encodeToString(data), Charsets.UTF_8)
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}
