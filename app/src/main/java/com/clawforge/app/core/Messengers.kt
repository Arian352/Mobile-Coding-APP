package com.clawforge.app.core

import org.json.JSONObject

/** Anbindung an WhatsApp (Meta Cloud API) und Telegram (Senden). */
object Messengers {

    /** Sendet eine Textnachricht über die WhatsApp Cloud API. */
    fun sendWhatsApp(text: String): String {
        if (!Store.whatsappEnabled) return "WhatsApp ist deaktiviert (Einstellungen)."
        val token = Store.whatsappToken
        val phoneId = Store.whatsappPhoneId
        val to = Store.whatsappTo
        if (token.isBlank() || phoneId.isBlank() || to.isBlank()) {
            return "WhatsApp ist nicht vollständig konfiguriert (Token, Telefon-ID und Empfänger nötig)."
        }
        return try {
            val body = JSONObject()
                .put("messaging_product", "whatsapp")
                .put("to", to)
                .put("type", "text")
                .put("text", JSONObject().put("body", text.take(4000)))
            Net.postJson(
                "https://graph.facebook.com/v19.0/$phoneId/messages",
                body.toString(),
                mapOf("Authorization" to "Bearer $token")
            )
            "WhatsApp-Nachricht gesendet an $to."
        } catch (e: Exception) {
            "WhatsApp-Fehler: ${e.message}"
        }
    }

    /** Sendet eine Telegram-Nachricht an einen Chat. */
    fun sendTelegram(chatId: String, text: String): String {
        val token = Store.telegramToken
        if (token.isBlank()) return "Kein Telegram-Bot-Token hinterlegt."
        return try {
            Net.postForm(
                "https://api.telegram.org/bot$token/sendMessage",
                mapOf("chat_id" to chatId, "text" to text.take(4000))
            )
            "Telegram-Nachricht gesendet."
        } catch (e: Exception) {
            "Telegram-Fehler: ${e.message}"
        }
    }
}
