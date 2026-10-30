package com.example.retouchjobs.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Загружает страницу объявления и достаёт из неё контакты. */
class ContactFetcher(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build(),
) {

    suspend fun fetch(url: String): List<Contact> = withContext(Dispatchers.IO) {
        val page = download(url) ?: throw IOException("Страница не загрузилась")
        var found = ContactExtractor.fromHtml(page)

        if (found.isEmpty()) {
            val contactsUrl = ContactExtractor.contactPageUrl(page, url)
            val contactsPage = contactsUrl?.let { download(it) }
            if (contactsPage != null) found = ContactExtractor.fromHtml(contactsPage)
        }
        found
    }

    private fun download(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.5")
                .build()
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val type = resp.header("Content-Type").orEmpty()
                if (type.isNotEmpty() && !type.contains("html", true) && !type.contains("text", true)) {
                    return@use null
                }
                resp.peekBody(MAX_BYTES).string()
            }
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private companion object {
        const val MAX_BYTES = 1_500_000L
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/126.0 Mobile Safari/537.36"
    }
}
