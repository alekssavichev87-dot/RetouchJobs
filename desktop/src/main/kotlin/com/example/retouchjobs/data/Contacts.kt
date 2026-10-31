package com.example.retouchjobs.data

import kotlinx.serialization.Serializable
import java.net.URLDecoder
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

enum class ContactType { PHONE, WHATSAPP, TELEGRAM, EMAIL, VK }

@Serializable
data class Contact(
    val type: ContactType,
    val label: String,
    val uri: String,
)

/** Достаёт публичные контакты для связи из текста или HTML объявления. */
object ContactExtractor {

    private val hrefRe = Regex("""href\s*=\s*["']([^"'#][^"']*)["']""", RegexOption.IGNORE_CASE)
    private val scriptRe = Regex(
        """<(script|style|noscript|svg)[^>]*>.*?</\1>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val tagRe = Regex("""<[^>]+>""", RegexOption.DOT_MATCHES_ALL)
    private val spaceRe = Regex("""\s+""")

    private val ruPhoneRe =
        Regex("""(?<!\d)(?:\+7|8)[\s\-().]*[3489]\d{2}[\s\-().]*\d{3}[\s\-.]*\d{2}[\s\-.]*\d{2}(?!\d)""")
    private val intlPhoneRe = Regex("""(?<![\w+])\+(?!7)\d[\d\s\-().]{8,16}\d""")
    private val emailRe =
        Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9\-]+(?:\.[A-Za-z0-9\-]+)*\.[A-Za-z]{2,}""")
    private val tmeRe =
        Regex("""(?:https?://)?(?:t|telegram)\.me/(\+?[A-Za-z0-9_]{4,64})""", RegexOption.IGNORE_CASE)
    private val tgHandleRe = Regex(
        """(?:telegram|телеграм|tg|тг)\W{0,15}(@[A-Za-z][A-Za-z0-9_]{4,31})""",
        RegexOption.IGNORE_CASE,
    )
    private val waRe = Regex(
        """(?:wa\.me/|whatsapp\.com/send\?phone=)\+?(\d{10,15})""",
        RegexOption.IGNORE_CASE,
    )
    private val vkRe =
        Regex("""(?:https?://)?(?:m\.)?vk\.(?:com|ru)/([A-Za-z0-9_.]{3,})""", RegexOption.IGNORE_CASE)

    private val badTelegram = setOf(
        "share", "joinchat", "addstickers", "iv", "proxy", "socks", "login",
        "setlanguage", "addtheme", "bg", "invoice",
    )
    private val badVkPrefixes = listOf("share", "widget", "away", "feed", "login", "dev", "apps")
    private val badEmailParts = listOf(
        "noreply", "no-reply", "sentry", "example.", "wixpress", "@2x",
        ".png", ".jpg", ".jpeg", ".gif", ".svg", ".webp", ".css", ".js",
    )

    fun fromText(text: String): List<Contact> {
        val out = ArrayList<Contact>()
        for (m in ruPhoneRe.findAll(text)) phone(m.value)?.let { out.add(it) }
        for (m in intlPhoneRe.findAll(text)) phone(m.value)?.let { out.add(it) }
        for (m in waRe.findAll(text)) out.add(whatsapp(m.groupValues[1]))
        for (m in tmeRe.findAll(text)) telegram(m.groupValues[1])?.let { out.add(it) }
        for (m in tgHandleRe.findAll(text)) {
            telegram(m.groupValues[1].removePrefix("@"))?.let { out.add(it) }
        }
        for (m in emailRe.findAll(text)) email(m.value)?.let { out.add(it) }
        for (m in vkRe.findAll(text)) vk(m.groupValues[1])?.let { out.add(it) }
        return finalize(out)
    }

    fun fromHtml(html: String): List<Contact> {
        val out = ArrayList<Contact>()

        for (m in hrefRe.findAll(html)) {
            val href = percentDecode(m.groupValues[1].replace("&amp;", "&").trim())
            val lower = href.lowercase()
            if (lower.startsWith("tel:")) {
                phone(href.substring(4).trim())?.let { out.add(it) }
            } else if (lower.startsWith("mailto:")) {
                val address = href.substring(7).substringBefore('?').trim()
                if (emailRe.matchEntire(address) != null) email(address)?.let { out.add(it) }
            } else {
                waRe.find(href)?.let { out.add(whatsapp(it.groupValues[1])) }
                tmeRe.find(href)?.let { r -> telegram(r.groupValues[1])?.let { out.add(it) } }
                vkRe.find(href)?.let { r -> vk(r.groupValues[1])?.let { out.add(it) } }
            }
        }

        out.addAll(fromText(htmlToText(html)))
        return finalize(out)
    }

    /** Раскодирует %XX, не превращая «+» в пробел (в телефонах «+» значим). */
    private fun percentDecode(s: String): String =
        try {
            URLDecoder.decode(s.replace("+", "%2B"), "UTF-8")
        } catch (e: IllegalArgumentException) {
            s
        }

    fun merge(a: List<Contact>, b: List<Contact>): List<Contact> = finalize(a + b)

    fun htmlToText(html: String): String =
        html.replace(scriptRe, " ")
            .replace(tagRe, " ")
            .replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .replace("&#43;", "+")
            .replace("&plus;", "+")
            .replace("&amp;", "&")
            .replace(spaceRe, " ")

    private fun phone(raw: String): Contact? {
        val digits = raw.filter { it.isDigit() }
        val plus = raw.trimStart().startsWith("+")
        if (plus && !digits.startsWith("7")) {
            if (digits.length !in 10..15) return null
            return Contact(ContactType.PHONE, "+$digits", "tel:+$digits")
        }
        val d = when {
            digits.length == 11 && digits[0] == '8' -> "7" + digits.drop(1)
            digits.length == 11 && digits[0] == '7' -> digits
            else -> return null
        }
        val label = "+7 ${d.substring(1, 4)} ${d.substring(4, 7)}-${d.substring(7, 9)}-${d.substring(9)}"
        return Contact(ContactType.PHONE, label, "tel:+$d")
    }

    private fun email(raw: String): Contact? {
        val lower = raw.lowercase()
        if (badEmailParts.any { it in lower }) return null
        return Contact(ContactType.EMAIL, raw, "mailto:$raw")
    }

    private fun telegram(name: String): Contact? {
        if (name.lowercase() in badTelegram) return null
        val label = if (name.startsWith("+")) "t.me/$name" else "@$name"
        return Contact(ContactType.TELEGRAM, label, "https://t.me/$name")
    }

    private fun whatsapp(digits: String) =
        Contact(ContactType.WHATSAPP, "WhatsApp +$digits", "https://wa.me/$digits")

    private fun vk(name: String): Contact? {
        val lower = name.lowercase()
        if (badVkPrefixes.any { lower.startsWith(it) } || ".php" in lower) return null
        return Contact(ContactType.VK, "vk.com/$name", "https://vk.com/$name")
    }

    private fun finalize(list: List<Contact>): List<Contact> {
        val unique = list.distinctBy { it.uri.lowercase() }
        return ContactType.entries.flatMap { t -> unique.filter { it.type == t }.take(3) }
    }

    /** Ищет на странице ссылку на раздел «Контакты» того же сайта. */
    private val linkRe = Regex(
        """<a\s[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.{0,80}?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun contactPageUrl(html: String, base: String): String? {
        val baseUrl = base.toHttpUrlOrNull() ?: return null
        for (m in linkRe.findAll(html)) {
            val href = m.groupValues[1].trim()
            val text = m.groupValues[2].lowercase()
            val hrefLower = href.lowercase()
            val looksLikeContacts = "контакт" in text || "contact" in text ||
                "kontakt" in hrefLower || "contact" in hrefLower
            if (!looksLikeContacts) continue
            val resolved = baseUrl.resolve(href) ?: continue
            if (resolved.host == baseUrl.host && resolved.toString() != base) {
                return resolved.toString()
            }
        }
        return null
    }
}
