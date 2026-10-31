package com.example.retouchjobs.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Что известно о том, открыта ли вакансия. CONFIRMED ставится только при достаточных
 * доказательствах; отсутствие данных — это UNVERIFIED, а не «открыта».
 */
@Serializable
enum class ListingStatus { CONFIRMED, UNVERIFIED, STALE, CLOSED }

/** Можно ли работать удалённо. Если на странице нет определённых данных — UNKNOWN. */
@Serializable
enum class RemoteStatus { YES, NO, UNKNOWN }

@Serializable
data class PageCheck(
    val status: ListingStatus,
    val remote: RemoteStatus,
    val postedDaysAgo: Int? = null,
    /** Короткое пояснение, на чём основан вывод. */
    val note: String? = null,
)

/** Анализ загруженной страницы объявления. Никаких предположений: нет данных — «не установлено». */
object PageAnalyzer {

    /** Старше этого срока вакансия без явной даты окончания считается возможно устаревшей. */
    const val FRESH_DAYS = 60

    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    private val ldRe = Regex(
        """<script[^>]*type\s*=\s*["']application/ld\+json["'][^>]*>(.*?)</script>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val metaDateA = Regex(
        """<meta[^>]+(?:property|name|itemprop)\s*=\s*["'](?:article:published_time|datePublished|datePosted|pubdate|date)["'][^>]*content\s*=\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE,
    )
    private val metaDateB = Regex(
        """<meta[^>]+content\s*=\s*["']([^"']+)["'][^>]*(?:property|name|itemprop)\s*=\s*["'](?:article:published_time|datePublished|datePosted|pubdate|date)["']""",
        RegexOption.IGNORE_CASE,
    )

    private val CLOSED_MARKERS = listOf(
        "вакансия закрыта", "вакансия в архиве", "вакансия неактуальна", "вакансия не актуальна",
        "вакансия снята", "вакансия больше не актуальна", "вакансия уже закрыта",
        "набор закрыт", "набор завершён", "набор завершен", "позиция закрыта", "позиция закрыта",
        "приём откликов завершён", "прием откликов завершен", "приём заявок завершён",
        "объявление снято", "объявление закрыто", "объявление неактуально", "объявление не актуально",
        "страница не найдена", "мы больше не принимаем",
        "no longer accepting", "this job is no longer", "position has been filled",
        "job has expired", "this job has expired", "vacancy is closed", "applications are closed",
        "this position is closed",
    )

    /** Признаки реального описания вакансии. */
    private val OFFER_WORDS = listOf(
        "обязанности", "требования", "условия", "зарплата", "оплата", "оклад",
        "график работы", "мы предлагаем", "responsibilities", "requirements", "we offer", "salary",
    )

    /** Признаки того, что на странице есть способ откликнуться. */
    private val APPLY_WORDS = listOf(
        "откликнуться", "откликнитесь", "отправить отклик", "отправить резюме", "отправьте резюме",
        "присылайте резюме", "высылайте резюме", "пришлите резюме",
        "apply now", "apply for", "send your cv", "send your resume", "how to apply",
    )

    private val REMOTE_YES = listOf(
        "удалённая работа", "удаленная работа", "удалённо", "удаленно", "удалёнка", "удаленка",
        "remote", "дистанцион", "из любой точки", "работа из дома", "work from home", "wfh",
    )
    private val REMOTE_NO = listOf(
        "без удалённой", "без удаленной", "без удалёнки", "без удаленки",
        "удалённая работа невозможна", "удаленная работа невозможна",
        "удалённой работы нет", "удаленной работы нет", "удалёнка не", "удаленка не",
        "только офис", "только в офисе", "работа только в офисе",
        "office only", "on-site only", "onsite only", "no remote", "not remote",
    )

    /** Удалённая работа по короткому тексту (заголовок и сниппет). Нет данных — UNKNOWN. */
    fun remoteFromText(text: String): RemoteStatus {
        val t = text.lowercase()
        return when {
            REMOTE_NO.any { it in t } -> RemoteStatus.NO
            REMOTE_YES.any { it in t } -> RemoteStatus.YES
            else -> RemoteStatus.UNKNOWN
        }
    }

    fun analyze(
        html: String,
        httpStatus: Int = 200,
        today: LocalDate = LocalDate.now(),
    ): PageCheck {
        if (httpStatus == 404 || httpStatus == 410) {
            return PageCheck(
                status = ListingStatus.CLOSED,
                remote = RemoteStatus.UNKNOWN,
                note = "Страница удалена (HTTP $httpStatus)",
            )
        }

        val lower = ContactExtractor.htmlToText(html).lowercase()

        val postings = mutableListOf<JsonObject>()
        for (m in ldRe.findAll(html)) {
            runCatching { findJobPostings(json.parseToJsonElement(m.groupValues[1].trim()), postings) }
        }
        val posting = postings.firstOrNull()

        val validThrough = parseDate(posting?.text("validThrough"))
        val posted = parseDate(posting?.text("datePosted")) ?: parseDate(metaDate(html))
        val ageDays = posted?.let { ChronoUnit.DAYS.between(it, today).toInt().coerceAtLeast(0) }

        val remote = if (posting?.text("jobLocationType").equals("TELECOMMUTE", ignoreCase = true)) {
            RemoteStatus.YES
        } else {
            remoteFromText(lower)
        }
        val geo = names(posting?.get("applicantLocationRequirements")).take(3).joinToString(", ")
        val geoNote = geo.takeIf { it.isNotEmpty() }?.let { "Кого ищут: $it" }

        fun result(status: ListingStatus, note: String?) = PageCheck(
            status = status,
            remote = remote,
            postedDaysAgo = ageDays,
            note = listOfNotNull(note, geoNote).joinToString(" · ").ifEmpty { null },
        )

        if (validThrough != null && validThrough.isBefore(today)) {
            return result(ListingStatus.CLOSED, "Срок вакансии истёк: $validThrough")
        }
        val closedMarker = CLOSED_MARKERS.firstOrNull { it in lower }
        if (closedMarker != null) {
            return result(ListingStatus.CLOSED, "Признак закрытия: «$closedMarker»")
        }

        val stillValid = validThrough != null
        val hasApply = APPLY_WORDS.any { it in lower }
        val hasOffer = posting != null || OFFER_WORDS.count { it in lower } >= 2
        val fresh = ageDays != null && ageDays <= FRESH_DAYS

        return when {
            !stillValid && ageDays != null && ageDays > FRESH_DAYS ->
                result(ListingStatus.STALE, "Опубликована давно")

            posting != null && (stillValid || fresh) ->
                result(ListingStatus.CONFIRMED, null)

            hasOffer && hasApply && fresh ->
                result(ListingStatus.CONFIRMED, null)

            else -> {
                val reasons = buildList {
                    if (!hasOffer) add("нет описания вакансии")
                    if (ageDays == null) add("не найдена дата публикации")
                    if (!hasApply) add("нет способа откликнуться")
                }
                result(ListingStatus.UNVERIFIED, reasons.joinToString(", ").replaceFirstChar { it.uppercase() })
            }
        }
    }

    private fun findJobPostings(el: JsonElement, out: MutableList<JsonObject>) {
        when (el) {
            is JsonArray -> el.forEach { findJobPostings(it, out) }
            is JsonObject -> {
                val type = el["@type"]
                val isJob = when (type) {
                    is JsonPrimitive -> type.contentOrNull == "JobPosting"
                    is JsonArray -> type.any { (it as? JsonPrimitive)?.contentOrNull == "JobPosting" }
                    else -> false
                }
                if (isJob) out.add(el)
                el["@graph"]?.let { findJobPostings(it, out) }
            }
            else -> Unit
        }
    }

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun names(el: JsonElement?): List<String> = when (el) {
        is JsonArray -> el.flatMap { names(it) }
        is JsonObject -> listOfNotNull(el.text("name"))
        is JsonPrimitive -> listOfNotNull(el.contentOrNull)
        else -> emptyList()
    }

    private fun metaDate(html: String): String? =
        metaDateA.find(html)?.groupValues?.get(1) ?: metaDateB.find(html)?.groupValues?.get(1)

    private fun parseDate(s: String?): LocalDate? =
        s?.trim()?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}
