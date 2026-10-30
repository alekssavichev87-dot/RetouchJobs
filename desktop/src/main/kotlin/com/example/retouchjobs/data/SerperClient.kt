package com.example.retouchjobs.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class SerperException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { NO_KEY, BAD_KEY, LIMIT, NETWORK, SERVER }
}

class SerperClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    suspend fun search(apiKey: String, query: String, period: Period): List<OrganicResult> =
        withContext(Dispatchers.IO) {
            val payload = json.encodeToString(SerperRequest(q = query, tbs = period.tbs))
            val request = Request.Builder()
                .url("https://google.serper.dev/search")
                .header("X-API-KEY", apiKey)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            try {
                http.newCall(request).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    when {
                        resp.code == 401 || resp.code == 403 -> throw SerperException(
                            SerperException.Kind.BAD_KEY,
                            "Serper не принял ключ. Проверьте его в настройках.",
                        )
                        resp.code == 429 -> throw SerperException(
                            SerperException.Kind.LIMIT,
                            "Лимит запросов Serper исчерпан. Попробуйте позже.",
                        )
                        resp.code == 400 -> throw SerperException(
                            SerperException.Kind.SERVER,
                            "Serper отклонил запрос (возможно, закончились кредиты): $body",
                        )
                        !resp.isSuccessful -> throw SerperException(
                            SerperException.Kind.SERVER,
                            "Ошибка Serper: HTTP ${resp.code}",
                        )
                        else -> json.decodeFromString<SerperResponse>(body).organic
                    }
                }
            } catch (e: IOException) {
                throw SerperException(
                    SerperException.Kind.NETWORK,
                    "Нет соединения с интернетом.",
                )
            }
        }
}
