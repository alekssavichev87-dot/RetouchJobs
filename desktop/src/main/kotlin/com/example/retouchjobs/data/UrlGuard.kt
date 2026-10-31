package com.example.retouchjobs.data

import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Защита от SSRF: адреса страниц берутся из результатов поиска, то есть из чужих данных,
 * поэтому приложение не должно по ним обращаться к устройствам в локальной сети.
 *
 * Что проверяется:
 *  - схема только http/https, порт только 80/443;
 *  - запрещены localhost, имена без точки, .local/.internal и IP-адреса из частных диапазонов;
 *  - имена разрешаются через свой DNS: если хоть один адрес частный, соединение не открывается;
 *  - проверка срабатывает на КАЖДОМ перенаправлении (сетевой перехватчик OkHttp).
 */
object UrlGuard {

    private val allowedPorts = setOf(80, 443)
    private val ipv4Literal = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

    /** Публичный ли адрес. Всё локальное, частное и служебное считается недопустимым. */
    fun isPublic(a: InetAddress): Boolean {
        if (a.isAnyLocalAddress || a.isLoopbackAddress || a.isLinkLocalAddress ||
            a.isSiteLocalAddress || a.isMulticastAddress
        ) return false

        val b = a.address
        if (b.size == 4) {
            val o0 = b[0].toInt() and 0xFF
            val o1 = b[1].toInt() and 0xFF
            val o2 = b[2].toInt() and 0xFF
            if (o0 == 0) return false                                   // 0.0.0.0/8
            if (o0 == 100 && o1 in 64..127) return false                // 100.64.0.0/10 (CGNAT)
            if (o0 == 192 && o1 == 0 && o2 == 0) return false           // 192.0.0.0/24
            if (o0 == 198 && (o1 == 18 || o1 == 19)) return false       // 198.18.0.0/15
            if (o0 >= 240) return false                                 // зарезервировано и broadcast
        } else if (b.size == 16) {
            if ((b[0].toInt() and 0xFE) == 0xFC) return false           // fc00::/7 (ULA)
        }
        return true
    }

    /** Проверка адреса до запроса. Бросает IOException, если адрес не разрешён. */
    fun requireAllowed(url: HttpUrl) {
        if (url.scheme != "http" && url.scheme != "https") {
            throw IOException("Схема не разрешена: ${url.scheme}")
        }
        if (url.port !in allowedPorts) {
            throw IOException("Порт не разрешён: ${url.port}")
        }

        val host = url.host.lowercase()
        val isIpLiteral = host.contains(':') || ipv4Literal.matches(host)
        if (isIpLiteral) {
            val addr = try {
                InetAddress.getByName(host) // для IP-литерала DNS не используется
            } catch (e: UnknownHostException) {
                throw IOException("Некорректный адрес")
            }
            if (!isPublic(addr)) throw IOException("Адрес не разрешён")
            return
        }

        val internalName = host == "localhost" ||
            host.endsWith(".localhost") ||
            host.endsWith(".local") ||
            host.endsWith(".internal") ||
            host.endsWith(".lan") ||
            !host.contains('.')
        if (internalName) throw IOException("Адрес не разрешён")
    }

    /** DNS, который отказывает, если имя указывает хотя бы на один непубличный адрес. */
    private object GuardedDns : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val all = Dns.SYSTEM.lookup(hostname)
            if (all.any { !isPublic(it) }) {
                throw UnknownHostException("Адрес не разрешён")
            }
            return all
        }
    }

    /** HTTP-клиент для загрузки чужих страниц: с проверкой каждого адреса и редиректа. */
    fun safeClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .dns(GuardedDns)
        .addNetworkInterceptor(
            Interceptor { chain ->
                requireAllowed(chain.request().url)
                chain.proceed(chain.request())
            },
        )
        .build()
}
