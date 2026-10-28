package com.example.retouchjobs.data

import kotlinx.serialization.Serializable

@Serializable
data class SerperRequest(
    val q: String,
    val gl: String = "ru",
    val hl: String = "ru",
    val num: Int = 10,
    val tbs: String? = null,
)

@Serializable
data class SerperResponse(
    val organic: List<OrganicResult> = emptyList(),
)

@Serializable
data class OrganicResult(
    val title: String = "",
    val link: String = "",
    val snippet: String = "",
    val date: String? = null,
    val position: Int? = null,
)

enum class Period(val label: String, val tbs: String) {
    DAY("Сутки", "qdr:d"),
    WEEK("Неделя", "qdr:w"),
    MONTH("Месяц", "qdr:m"),
    YEAR("Год", "qdr:y"),
}

/**
 * Раздел поиска. [primary] — главные слова темы, [context] — уточняющие.
 * Первое и второе вместе дают «точное совпадение», одно первое — «по теме».
 */
enum class Category(
    val tabLabel: String,
    val screenTitle: String,
    val heroTitle: String,
    val heroText: String,
    val queries: List<String>,
    val primary: List<String>,
    val context: List<String>,
    val exactLabel: String,
    val goodLabel: String,
) {
    RETOUCH(
        tabLabel = "Ретушь",
        screenTitle = "Ретушь для памятников",
        heroTitle = "Актуальные вакансии ретушёра",
        heroText = "Ретушь портретов для гравировки на памятниках. " +
            "Ищем по всему интернету, кроме крупных job-бордов.",
        queries = listOf(
            "вакансия ретушер фото для гравировки на памятниках",
            "ретушер портретов для памятников требуется",
            "ретушь фотографий для гравировки на граните работа",
            "художник-ретушер памятники лазерная гравировка вакансия",
        ),
        primary = listOf("ретуш", "retouch"),
        context = listOf("памятник", "гравир", "гранит", "мемориал", "ритуал", "надгроб"),
        exactLabel = "Точное совпадение",
        goodLabel = "Про ретушь",
    ),
    DEV(
        tabLabel = "Android / вайбкод",
        screenTitle = "Android и вайбкодинг",
        heroTitle = "Вакансии для Android-разработчика и вайбкодера",
        heroText = "Kotlin, Jetpack Compose и разработка с помощью ИИ. " +
            "Ищем по всему интернету, кроме крупных job-бордов.",
        queries = listOf(
            "вакансия android разработчик kotlin jetpack compose",
            "вакансия вайбкодер vibe coding разработчик с ИИ",
            "требуется android developer kotlin",
            "vibe coder vacancy hiring AI-assisted developer",
        ),
        primary = listOf(
            "android", "андроид", "kotlin", "котлин",
            "вайбкод", "вайб-код", "vibe cod", "vibecod", "vibe-cod",
        ),
        context = listOf(
            "compose", "jetpack", "kotlin", "котлин", "вайбкод", "вайб-код", "vibe",
            "cursor", "claude", "нейросет", "llm", "copilot",
        ),
        exactLabel = "Точное совпадение",
        goodLabel = "По теме",
    ),
}

enum class Match { EXACT, GOOD, WEAK }

data class Vacancy(
    val title: String,
    val url: String,
    val snippet: String,
    val date: String?,
    val domain: String,
    val match: Match,
    val isVacancy: Boolean,
    val rank: Int,
    val contacts: List<Contact> = emptyList(),
)
