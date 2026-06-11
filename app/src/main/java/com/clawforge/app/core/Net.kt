package com.clawforge.app.core

import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Einfache HTTP-Hilfsfunktionen (auch für Telegram-Long-Polling). */
object Net {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(75, TimeUnit.SECONDS)
        .build()

    fun get(url: String): String {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}: ${text.take(400)}")
            return text
        }
    }

    fun postForm(url: String, fields: Map<String, String>): String {
        val body = FormBody.Builder().apply { fields.forEach { (k, v) -> add(k, v) } }.build()
        val req = Request.Builder().url(url).post(body).build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}: ${text.take(400)}")
            return text
        }
    }

    fun postJson(url: String, json: String, headers: Map<String, String> = emptyMap()): String {
        val req = Request.Builder().url(url)
            .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}: ${text.take(400)}")
            return text
        }
    }
}
