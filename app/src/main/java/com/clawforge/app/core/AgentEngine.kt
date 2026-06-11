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

/**
 * Der eigentliche Agent: baut den System-Prompt aus den Regeln, ruft die KI auf
 * und führt Werkzeug-Aufrufe (```tool-Blöcke) aus, bis eine finale Antwort kommt.
 */
object AgentEngine {
    private const val MAX_STEPS = 8
    private val toolRegex = Regex("```tool\\s*\\n([\\s\\S]*?)```")

    fun systemPrompt(): String = buildString {
        appendLine("Du bist ClawForge, ein autonomer KI-Agent auf einem Android-Handy.")
        appendLine("Du kannst Projekte und Dateien anlegen, Webseiten und App-Quellcode erstellen,")
        appendLine("das Internet nutzen, Geräte im lokalen Netzwerk finden und APK-Builds starten.")
        appendLine()
        appendLine("## Regeln des Nutzers (Datei mit kleinerer Nummer = höhere Priorität)")
        appendLine(RulesManager.combined())
        appendLine()
        appendLine("## Werkzeuge")
        appendLine("Um ein Werkzeug zu benutzen, antworte mit GENAU EINEM Block in diesem Format (und sonst nichts):")
        appendLine("```tool")
        appendLine("{\"name\": \"write_file\", \"args\": {\"project\": \"meine-website\", \"path\": \"index.html\", \"content\": \"...\"}}")
        appendLine("```")
        appendLine("Du erhältst danach eine Nachricht mit TOOL_RESULT und kannst weitere Werkzeuge nutzen")
        appendLine("oder dem Nutzer final antworten (dann OHNE tool-Block).")
        appendLine()
        appendLine("Verfügbare Werkzeuge:")
        appendLine("- create_project  args: {\"name\"} – neues Projekt anlegen")
        appendLine("- list_projects   args: {} – alle Projekte auflisten")
        appendLine("- list_files      args: {\"project\"} – Dateien eines Projekts auflisten")
        appendLine("- read_file       args: {\"project\", \"path\"} – Datei lesen")
        appendLine("- write_file      args: {\"project\", \"path\", \"content\"} – Datei schreiben (legt Ordner automatisch an)")
        appendLine("- delete_file     args: {\"project\", \"path\"} – Datei löschen")
        appendLine("- http_get        args: {\"url\"} – Webseite/API abrufen (nur wenn Internet-Zugriff aktiviert ist)")
        appendLine("- scan_network    args: {} – Geräte im WLAN finden (nur wenn Netzwerk-Zugriff aktiviert ist)")
        appendLine("- trigger_apk_build args: {} – startet den APK-Build per GitHub Actions (GitHub-Token nötig)")
        appendLine("- send_whatsapp   args: {\"text\"} – Nachricht per WhatsApp senden (wenn konfiguriert)")
        appendLine()
        appendLine("Hinweis: Für Webseiten lege HTML/CSS/JS-Dateien in einem Projekt an.")
        appendLine("Für Android-Apps lege den Quellcode in einem Projekt an und nutze trigger_apk_build.")
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
                "Fehler beim Werkzeug-Aufruf: ${e.message}"
            }
            msgs.add(ChatMsg("assistant", reply))
            msgs.add(ChatMsg("user", "TOOL_RESULT:\n${result.take(8000)}"))
        }
        return "Abgebrochen: maximale Anzahl an Werkzeug-Schritten erreicht."
    }

    private suspend fun executeTool(call: JSONObject): String {
        val name = call.getString("name")
        val args = call.optJSONObject("args") ?: JSONObject()
        return when (name) {
            "create_project" -> ProjectsManager.create(args.getString("name"))
            "list_projects" -> ProjectsManager.list().joinToString("\n") { it.name }
                .ifBlank { "Keine Projekte vorhanden." }
            "list_files" -> ProjectsManager.listFiles(args.getString("project"))
            "read_file" -> ProjectsManager.readFile(args.getString("project"), args.getString("path"))
            "write_file" -> ProjectsManager.writeFile(
                args.getString("project"), args.getString("path"), args.getString("content")
            )
            "delete_file" -> ProjectsManager.deleteFile(args.getString("project"), args.getString("path"))
            "http_get" -> httpGet(args.getString("url"))
            "scan_network" -> scanNetwork()
            "trigger_apk_build" -> triggerApkBuild()
            "send_whatsapp" -> Messengers.sendWhatsApp(args.getString("text"))
            else -> "Unbekanntes Werkzeug: $name"
        }
    }

    private suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
        if (!Store.internetEnabled) {
            "Internet-Zugriff ist deaktiviert. Der Nutzer kann ihn in den Einstellungen aktivieren."
        } else {
            Net.get(url).take(8000)
        }
    }

    private suspend fun scanNetwork(): String {
        if (!Store.networkAccessEnabled) {
            return "Netzwerk-Zugriff ist deaktiviert. Der Nutzer kann ihn in den Einstellungen aktivieren."
        }
        val local = localIp() ?: return "Keine lokale WLAN-IP gefunden."
        val prefix = local.substringBeforeLast(".")
        val found = coroutineScope {
            (1..254).map { i ->
                async(Dispatchers.IO) {
                    val ip = "$prefix.$i"
                    try {
                        if (InetAddress.getByName(ip).isReachable(300)) ip else null
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }
        return if (found.isEmpty()) "Keine Geräte im Netz $prefix.0/24 gefunden."
        else "Eigene IP: $local\nGefundene Geräte:\n" + found.joinToString("\n")
    }

    private fun localIp(): String? =
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
            ?.hostAddress

    private suspend fun triggerApkBuild(): String = withContext(Dispatchers.IO) {
        val token = Store.githubToken
        val repo = Store.githubRepo
        if (token.isBlank() || repo.isBlank()) {
            return@withContext "GitHub-Token oder Repository fehlt. Bitte in den Einstellungen unter 'APK-Builds' eintragen (Format: benutzer/repo)."
        }
        val body = JSONObject().put("ref", "main")
        Net.postJson(
            "https://api.github.com/repos/$repo/actions/workflows/build-apk.yml/dispatches",
            body.toString(),
            mapOf(
                "Authorization" to "Bearer $token",
                "Accept" to "application/vnd.github+json"
            )
        )
        "APK-Build gestartet! Fortschritt und Download: https://github.com/$repo/actions"
    }
}
