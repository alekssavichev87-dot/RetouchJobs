package com.example.retouchjobs.data

import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class VacancyRepository(private val client: SerperClient) {

    suspend fun search(s: Settings, category: Category): List<Vacancy> = coroutineScope {
        if (s.apiKey.isBlank()) {
            throw SerperException(
                SerperException.Kind.NO_KEY,
                "Сначала добавьте API-ключ Serper в настройках.",
            )
        }

        val blocked = parseDomains(s.blockedDomains)
        val exclude = blocked.joinToString(" ") { "-site:$it" }
        val suffix = if (s.remote) " удалённо" else ""

        val results = category.queries.map { base ->
            async {
                runCatching {
                    client.search(s.apiKey, "$base$suffix $exclude".trim(), s.period)
                }.onFailure { if (it is CancellationException) throw it }
            }
        }.awaitAll()

        val ok = results.mapNotNull { it.getOrNull() }
        if (ok.isEmpty()) {
            throw results.firstNotNullOfOrNull { it.exceptionOrNull() }
                ?: IllegalStateException("Пустой ответ")
        }

        ok.flatten()
            .mapNotNull { toVacancy(it, blocked, category) }
            .sortedWith(compareBy<Vacancy> { it.match.ordinal }.thenBy { it.rank })
            .distinctBy { it.url.substringBefore('#').trimEnd('/') }
    }

    private fun toVacancy(r: OrganicResult, blocked: List<String>, category: Category): Vacancy? {
        if (r.link.isBlank()) return null
        val host = Uri.parse(r.link).host?.lowercase()?.removePrefix("www.") ?: return null
        if (blocked.any { host == it || host.endsWith(".$it") }) return null

        val text = "${r.title} ${r.snippet}".lowercase()
        val hasPrimary = category.primary.any { it in text }
        val hasContext = category.context.any { it in text }
        val match = when {
            hasPrimary && hasContext -> Match.EXACT
            hasPrimary -> Match.GOOD
            else -> Match.WEAK
        }

        return Vacancy(
            title = r.title.trim(),
            url = r.link,
            snippet = r.snippet.trim(),
            date = r.date?.takeIf { it.isNotBlank() },
            domain = host,
            match = match,
            isVacancy = VACANCY_WORDS.any { it in text },
            rank = r.position ?: Int.MAX_VALUE,
            contacts = ContactExtractor.fromText("${r.title} ${r.snippet}"),
        )
    }

    companion object {
        private val VACANCY_WORDS = listOf(
            "вакан", "требуется", "ищем", "приглаша", "набираем", "ищу",
            "работа", "в штат", "зарплат", "оплата", "график", "удален", "удалён",
            "vacancy", "hiring", "we are looking", "job", "career",
        )

        fun parseDomains(raw: String): List<String> =
            raw.split(',', ';', '\n', ' ')
                .map {
                    it.trim().lowercase()
                        .removePrefix("https://")
                        .removePrefix("http://")
                        .removePrefix("www.")
                        .trimEnd('/')
                }
                .filter { it.contains('.') }
                .distinct()
    }
}
