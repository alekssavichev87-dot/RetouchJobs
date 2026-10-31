package com.example.retouchjobs.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class PageResult(val check: PageCheck, val contacts: List<Contact>)

/**
 * Загружает страницу объявления и достаёт из неё контакты, статус вакансии и условия удалёнки.
 * Клиент по умолчанию — защищённый: см. [UrlGuard].
 */
class PageFetcher(
    private val http: OkHttpClient = UrlGuard.safeClient(),
) {

    suspend fun fetch(url: String): PageResult = withContext(Dispatchers.IO) {
        val page = download(url) ?: throw IOException("Страница не загрузилась")

        // Страница удалена: контактов и анализа нет, вакансия закрыта.
        if (page.code == 404 || page.code == 410) {
            return@withContext PageResult(PageAnalyzer.analyze("", page.code), emptyList())
        }

        var contacts = ContactExtractor.fromHtml(page.body)
        if (contacts.isEmpty()) {
            val contactsUrl = ContactExtractor.contactPageUrl(page.body, url)
            val contactsPage = contactsUrl?.let { download(it) }
            if (contactsPage != null && contactsPage.code in 200..299) {
                contacts = ContactExtractor.fromHtml(contactsPage.body)
            }
        }
        PageResult(PageAnalyzer.analyze(page.body, page.code), contacts)
    }

    private class Page(val code: Int, val body: String)

    private fun download(url: String): Page? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.5")
                .build()
            http.newCall(request).execute().use { resp ->
                if (resp.code == 404 || resp.code == 410) return@use Page(resp.code, "")
                if (!resp.isSuccessful) return@use null
                val type = resp.header("Content-Type").orEmpty()
                if (type.isNotEmpty() && !type.contains("html", true) && !type.contains("text", true)) {
                    return@use null
                }
                Page(resp.code, resp.peekBody(MAX_BYTES).string())
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
