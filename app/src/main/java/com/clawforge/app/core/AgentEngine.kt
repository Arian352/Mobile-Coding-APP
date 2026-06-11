package com.clawforge.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object AgentEngine {
    private const val MAX_STEPS = 10
    private val toolRegex = Regex("```tool\\s*\\n([\\s\\S]*?)```")

    fun systemPrompt(): String = buildString {
        appendLine("Du bist ClawForge, ein autonomer KI-Agent direkt auf einem Android-Handy.")
        appendLine("Du hast echten Zugriff auf Telefon-Funktionen und kannst sie wirklich ausführen.")
        appendLine("Benutze die Werkzeuge aktiv und ohne Entschuldigung – sie funktionieren wirklich.")
        appendLine()
        appendLine("## Regeln des Nutzers")
        appendLine(RulesManager.combined())
        appendLine()
        appendLine("## Werkzeuge – Format (exakt so, kein Fließtext drumherum):")
        appendLine("```tool")
        appendLine("{\"name\": \"set_alarm\", \"args\": {\"hour\": 8, \"minute\": 30, \"label\": \"Aufstehen\"}}")
        appendLine("```")
        appendLine("Nach jedem Werkzeugaufruf bekommst du TOOL_RESULT und kannst weitermachen.")
        appendLine()
        appendLine("### TELEFON-WERKZEUGE (funktionieren wirklich auf dem Gerät)")
        appendLine("- set_alarm        {\"hour\":8,\"minute\":30,\"label\":\"…\"} – Wecker stellen")
        appendLine("- set_timer        {\"seconds\":300,\"label\":\"…\"} – Timer starten")
        appendLine("- open_url         {\"url\":\"https://…\"} – URL im Browser öffnen")
        appendLine("- dial_number      {\"number\":\"+491234567890\"} – Anruf-App öffnen")
        appendLine("- open_sms         {\"number\":\"+49…\",\"text\":\"…\"} – SMS-App öffnen")
        appendLine("- get_battery      {} – Akkustand abfragen")
        appendLine("- get_device_info  {} – Geräteinfo")
        appendLine("- vibrate          {\"ms\":500} – Handy vibrieren lassen")
        appendLine("- toggle_flashlight {} – Taschenlampe an/aus")
        appendLine("- set_volume       {\"type\":\"musik\",\"level\":50} – Lautstärke 0-100")
        appendLine("- open_app         {\"package\":\"com.whatsapp\"} – App öffnen")
        appendLine("- share_text       {\"text\":\"…\"} – Teilen-Dialog öffnen")
        appendLine()
        appendLine("### DATEI- & PROJEKT-WERKZEUGE")
        appendLine("- create_project   {\"name\":\"…\"} – neues Projekt anlegen")
        appendLine("- list_projects    {} – Projekte auflisten")
        appendLine("- list_files       {\"project\":\"…\"} – Dateien eines Projekts")
        appendLine("- read_file        {\"project\":\"…\",\"path\":\"…\"} – Datei lesen")
        appendLine("- write_file       {\"project\":\"…\",\"path\":\"…\",\"content\":\"…\"} – Datei schreiben")
        appendLine("- delete_file      {\"project\":\"…\",\"path\":\"…\"} – Datei löschen")
        appendLine()
        appendLine("### INTERNET & NETZWERK")
        appendLine("- http_get         {\"url\":\"…\"} – Webseite/API abrufen (Internet muss aktiv sein)")
        appendLine("- scan_network     {} – WLAN-Geräte scannen (Netzwerk-Zugriff muss aktiv sein)")
        appendLine()
        appendLine("### MESSAGING & BUILD")
        appendLine("- send_whatsapp    {\"text\":\"…\"} – WhatsApp Cloud API (wenn konfiguriert)")
        appendLine("- trigger_apk_build {} – APK via GitHub Actions bauen")
    }

    suspend fun run(history: List<ChatMsg>): String {
        val msgs = mutableListOf(ChatMsg("system", systemPrompt()))
        msgs.addAll(history)
        repeat(MAX_STEPS) {
            val reply = AiClient.chat(msgs)
            val match = toolRegex.find(reply) ?: return reply
            val result = try {
                executeTool(JSONObject(match.groupValues[1].trim()))
            } catch (e: Exception) {
                "Werkzeug-Fehler: ${e.message}"
            }
            msgs.add(ChatMsg("assistant", reply))
            msgs.add(ChatMsg("user", "TOOL_RESULT:\n${result.take(8000)}"))
        }
        return "Maximale Schritte erreicht."
    }

    private suspend fun executeTool(call: JSONObject): String {
        val name = call.getString("name")
        val a = call.optJSONObject("args") ?: JSONObject()
        return when (name) {
            // Telefon
            "set_alarm" -> PhoneTools.setAlarm(a.getInt("hour"), a.getInt("minute"), a.optString("label", "ClawForge"))
            "set_timer" -> PhoneTools.setTimer(a.getInt("seconds"), a.optString("label", "Timer"))
            "open_url" -> PhoneTools.openUrl(a.getString("url"))
            "dial_number" -> PhoneTools.dialNumber(a.getString("number"))
            "open_sms" -> PhoneTools.openSms(a.getString("number"), a.optString("text", ""))
            "get_battery" -> PhoneTools.getBattery()
            "get_device_info" -> PhoneTools.getDeviceInfo()
            "vibrate" -> PhoneTools.vibrate(a.optLong("ms", 500))
            "toggle_flashlight" -> PhoneTools.toggleFlashlight()
            "set_volume" -> PhoneTools.setVolume(a.getString("type"), a.getInt("level"))
            "open_app" -> PhoneTools.openApp(a.getString("package"))
            "share_text" -> PhoneTools.shareText(a.getString("text"))
            // Projekte
            "create_project" -> ProjectsManager.create(a.getString("name"))
            "list_projects" -> ProjectsManager.list().joinToString("\n") { it.name }.ifBlank { "Keine Projekte." }
            "list_files" -> ProjectsManager.listFiles(a.getString("project"))
            "read_file" -> ProjectsManager.readFile(a.getString("project"), a.getString("path"))
            "write_file" -> ProjectsManager.writeFile(a.getString("project"), a.getString("path"), a.getString("content"))
            "delete_file" -> ProjectsManager.deleteFile(a.getString("project"), a.getString("path"))
            // Internet
            "http_get" -> httpGet(a.getString("url"))
            "scan_network" -> scanNetwork()
            // Messaging
            "send_whatsapp" -> Messengers.sendWhatsApp(a.getString("text"))
            "trigger_apk_build" -> triggerApkBuild()
            else -> "Unbekanntes Werkzeug: $name"
        }
    }

    private suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
        if (!Store.internetEnabled) "Internet-Zugriff ist deaktiviert (Einstellungen → Berechtigungen)."
        else Net.get(url).take(8000)
    }

    private suspend fun scanNetwork(): String {
        if (!Store.networkAccessEnabled) return "Netzwerk-Scan ist deaktiviert (Einstellungen → Berechtigungen)."
        val local = localIp() ?: return "Keine lokale WLAN-IP gefunden."
        val prefix = local.substringBeforeLast(".")
        val found = coroutineScope {
            (1..254).map { i ->
                async(Dispatchers.IO) {
                    val ip = "$prefix.$i"
                    try { if (InetAddress.getByName(ip).isReachable(300)) ip else null } catch (_: Exception) { null }
                }
            }.awaitAll().filterNotNull()
        }
        return if (found.isEmpty()) "Keine Geräte in $prefix.0/24 gefunden."
        else "Eigene IP: $local\nGeräte:\n${found.joinToString("\n")}"
    }

    private fun localIp(): String? =
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
            ?.hostAddress

    private suspend fun triggerApkBuild(): String = withContext(Dispatchers.IO) {
        val token = Store.githubToken
        val repo = Store.githubRepo
        if (token.isBlank() || repo.isBlank()) return@withContext "GitHub-Token oder Repository fehlt (Einstellungen → APK-Builds)."
        Net.postJson(
            "https://api.github.com/repos/$repo/actions/workflows/build-apk.yml/dispatches",
            org.json.JSONObject().put("ref", "main").toString(),
            mapOf("Authorization" to "Bearer $token", "Accept" to "application/vnd.github+json")
        )
        "APK-Build gestartet! Fortschritt: https://github.com/$repo/actions"
    }
}
