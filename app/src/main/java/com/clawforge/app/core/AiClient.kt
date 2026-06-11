package com.clawforge.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ChatMsg(val role: String, val content: String)

/** Anbindung an die KI-Anbieter (Gemini, OpenAI, Anthropic, OpenRouter). */
object AiClient {
    val PROVIDERS = listOf("Anthropic", "Gemini", "OpenAI", "OpenRouter")

    fun defaultModels(provider: String): List<String> = when (provider) {
        "Gemini" -> listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash", "gemini-2.5-flash-lite")
        "OpenAI" -> listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1", "o3-mini")
        "Anthropic" -> listOf("claude-opus-4-8", "claude-sonnet-4-6", "claude-haiku-4-5")
        "OpenRouter" -> listOf(
            "google/gemini-2.0-flash-001",
            "openai/gpt-4o-mini",
            "meta-llama/llama-3.3-70b-instruct"
        )
        else -> listOf("")
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun chat(messages: List<ChatMsg>): String = withContext(Dispatchers.IO) {
        val provider = Store.provider
        val key = Store.apiKey(provider)
        if (key.isBlank()) {
            throw IOException("Kein API-Schlüssel für $provider hinterlegt. Bitte in den Einstellungen eintragen.")
        }
        val model = Store.model(provider)
        when (provider) {
            "Gemini" -> gemini(key, model, messages)
            "OpenAI" -> openAiCompatible("https://api.openai.com/v1/chat/completions", key, model, messages)
            "OpenRouter" -> openAiCompatible("https://openrouter.ai/api/v1/chat/completions", key, model, messages)
            "Anthropic" -> anthropic(key, model, messages)
            else -> throw IOException("Unbekannter Anbieter: $provider")
        }
    }

    private fun post(url: String, body: JSONObject, headers: Map<String, String>): JSONObject {
        val req = Request.Builder().url(url)
            .post(body.toString().toRequestBody(JSON_TYPE))
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}: ${text.take(400)}")
            return JSONObject(text)
        }
    }

    private fun gemini(key: String, model: String, messages: List<ChatMsg>): String {
        val body = JSONObject()
        val sys = messages.filter { it.role == "system" }.joinToString("\n\n") { it.content }
        if (sys.isNotBlank()) {
            body.put(
                "system_instruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", sys)))
            )
        }
        val contents = JSONArray()
        messages.filter { it.role != "system" }.forEach { m ->
            contents.put(
                JSONObject()
                    .put("role", if (m.role == "assistant") "model" else "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", m.content)))
            )
        }
        body.put("contents", contents)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
        val json = post(url, body, emptyMap())
        return json.getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts")
            .getJSONObject(0).getString("text")
    }

    private fun openAiCompatible(url: String, key: String, model: String, messages: List<ChatMsg>): String {
        val body = JSONObject()
            .put("model", model)
            .put("messages", JSONArray().apply {
                messages.forEach { put(JSONObject().put("role", it.role).put("content", it.content)) }
            })
        val json = post(url, body, mapOf("Authorization" to "Bearer $key"))
        return json.getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content")
    }

    private fun anthropic(key: String, model: String, messages: List<ChatMsg>): String {
        val sys = messages.filter { it.role == "system" }.joinToString("\n\n") { it.content }
        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", 8192)
        if (sys.isNotBlank()) body.put("system", sys)
        body.put("messages", JSONArray().apply {
            messages.filter { it.role != "system" }.forEach {
                put(JSONObject().put("role", it.role).put("content", it.content))
            }
        })
        val json = post(
            "https://api.anthropic.com/v1/messages", body,
            mapOf("x-api-key" to key, "anthropic-version" to "2023-06-01")
        )
        return json.getJSONArray("content").getJSONObject(0).getString("text")
    }
}
