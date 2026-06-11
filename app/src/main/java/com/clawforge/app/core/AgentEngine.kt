package com.clawforge.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object AgentEngine {
    private const val MAX_STEPS = 10

    // Matches a bare JSON object that contains a "tool" key, anywhere in the response
    private val toolRegex = Regex("""(?s)\{"tool"\s*:\s*"[^"]+".+?\}""")

    fun loadRules(): String {
        val dir = File("/sdcard/ClawForge/rules/")
        if (!dir.exists()) return RulesManager.combined()
        val external = dir.listFiles()
            ?.filter { it.name.endsWith(".txt") || it.name.endsWith(".md") }
            ?.sortedBy { it.name }
            ?.joinToString("\n---\n") { it.readText() }
            ?: ""
        val internal = RulesManager.combined()
        return listOf(external, internal).filter { it.isNotBlank() }.joinToString("\n---\n")
    }

    fun systemPrompt(): String = buildString {
        appendLine("You are ClawForge, an AI assistant powered by Claude (Anthropic) that can control everything on this Android device.")
        appendLine("You have real access to phone functions and can execute them truly.")
        appendLine("Use the tools actively and without apology - they work for real.")
        appendLine()
        appendLine("## User Rules")
        appendLine(loadRules())
        appendLine()
        appendLine("## TOOL USAGE")
        appendLine("When you want to use a tool, reply ONLY with this exact JSON format (nothing else around it):")
        appendLine("""{"tool": "toolName", "args": {"key": "value"}}""")
        appendLine("After each tool call you will receive TOOL_RESULT and can continue.")
        appendLine()
        appendLine("### PHONE TOOLS (work for real on the device)")
        appendLine("""- setAlarm      {"tool":"setAlarm","args":{"hour":8,"minute":30,"label":"Wake up"}}""")
        appendLine("""- setTimer      {"tool":"setTimer","args":{"seconds":300,"label":"Timer"}}""")
        appendLine("""- openUrl       {"tool":"openUrl","args":{"url":"https://..."}}""")
        appendLine("""- dialNumber    {"tool":"dialNumber","args":{"number":"+491234567890"}}""")
        appendLine("""- openSms       {"tool":"openSms","args":{"number":"+49...","text":"..."}}""")
        appendLine("""- getBattery    {"tool":"getBattery","args":{}}""")
        appendLine("""- getDeviceInfo {"tool":"getDeviceInfo","args":{}}""")
        appendLine("""- vibrate       {"tool":"vibrate","args":{"ms":500}}""")
        appendLine("""- toggleFlashlight {"tool":"toggleFlashlight","args":{}}""")
        appendLine("""- setVolume     {"tool":"setVolume","args":{"type":"music","level":50}}""")
        appendLine("""- openApp       {"tool":"openApp","args":{"packageName":"com.whatsapp"}}""")
        appendLine("""- shareText     {"tool":"shareText","args":{"text":"..."}}""")
        appendLine("""- updateApk     {"tool":"updateApk","args":{"downloadUrl":"https://github.com/.../releases/download/.../app.apk"}}""")
        appendLine()
        appendLine("### FILE & PROJECT TOOLS")
        appendLine("""- createProject {"tool":"createProject","args":{"name":"..."}}""")
        appendLine("""- listProjects  {"tool":"listProjects","args":{}}""")
        appendLine("""- listFiles     {"tool":"listFiles","args":{"project":"..."}}""")
        appendLine("""- readFile      {"tool":"readFile","args":{"project":"...","path":"..."}}""")
        appendLine("""- writeFile     {"tool":"writeFile","args":{"project":"...","path":"...","content":"..."}}""")
        appendLine("""- deleteFile    {"tool":"deleteFile","args":{"project":"...","path":"..."}}""")
        appendLine()
        appendLine("### INTERNET & NETWORK")
        appendLine("""- httpGet       {"tool":"httpGet","args":{"url":"..."}}""")
        appendLine("""- scanNetwork   {"tool":"scanNetwork","args":{}}""")
        appendLine()
        appendLine("### MESSAGING & BUILD")
        appendLine("""- sendWhatsapp  {"tool":"sendWhatsapp","args":{"text":"..."}}""")
        appendLine("""- triggerApkBuild {"tool":"triggerApkBuild","args":{}}""")
    }

    suspend fun run(history: List<ChatMsg>): String {
        val msgs = mutableListOf(ChatMsg("system", systemPrompt()))
        msgs.addAll(history)
        repeat(MAX_STEPS) {
            val reply = AiClient.chat(msgs)
            val match = toolRegex.find(reply) ?: return reply
            val toolJson = try {
                JSONObject(match.value)
            } catch (e: Exception) {
                return reply
            }
            val result = try {
                executeTool(toolJson)
            } catch (e: Exception) {
                "Tool error: ${e.message}"
            }
            msgs.add(ChatMsg("assistant", reply))
            msgs.add(ChatMsg("user", "TOOL_RESULT:\n${result.take(8000)}"))
        }
        return "Maximum steps reached."
    }

    private suspend fun executeTool(call: JSONObject): String {
        val name = call.getString("tool")
        val a = call.optJSONObject("args") ?: JSONObject()
        return when (name) {
            // Phone tools
            "setAlarm" -> PhoneTools.setAlarm(a.getInt("hour"), a.getInt("minute"), a.optString("label", "ClawForge"))
            "setTimer" -> PhoneTools.setTimer(a.getInt("seconds"), a.optString("label", "Timer"))
            "openUrl" -> PhoneTools.openUrl(a.getString("url"))
            "dialNumber" -> PhoneTools.dialNumber(a.getString("number"))
            "openSms" -> PhoneTools.openSms(a.getString("number"), a.optString("text", ""))
            "getBattery" -> PhoneTools.getBattery()
            "getDeviceInfo" -> PhoneTools.getDeviceInfo()
            "vibrate" -> PhoneTools.vibrate(a.optLong("ms", 500))
            "toggleFlashlight" -> PhoneTools.toggleFlashlight()
            "setVolume" -> PhoneTools.setVolume(a.getString("type"), a.getInt("level"))
            "openApp" -> PhoneTools.openApp(a.getString("packageName"))
            "shareText" -> PhoneTools.shareText(a.getString("text"))
            "updateApk" -> PhoneTools.updateApk(a.getString("downloadUrl"))
            // Projects
            "createProject" -> ProjectsManager.create(a.getString("name"))
            "listProjects" -> ProjectsManager.list().joinToString("\n") { it.name }.ifBlank { "No projects." }
            "listFiles" -> ProjectsManager.listFiles(a.getString("project"))
            "readFile" -> ProjectsManager.readFile(a.getString("project"), a.getString("path"))
            "writeFile" -> ProjectsManager.writeFile(a.getString("project"), a.getString("path"), a.getString("content"))
            "deleteFile" -> ProjectsManager.deleteFile(a.getString("project"), a.getString("path"))
            // Internet
            "httpGet" -> httpGet(a.getString("url"))
            "scanNetwork" -> scanNetwork()
            // Messaging
            "sendWhatsapp" -> Messengers.sendWhatsApp(a.getString("text"))
            "triggerApkBuild" -> triggerApkBuild()
            // Legacy aliases (backward compat with old tool names)
            "set_alarm" -> PhoneTools.setAlarm(a.getInt("hour"), a.getInt("minute"), a.optString("label", "ClawForge"))
            "set_timer" -> PhoneTools.setTimer(a.getInt("seconds"), a.optString("label", "Timer"))
            "open_url" -> PhoneTools.openUrl(a.getString("url"))
            "dial_number" -> PhoneTools.dialNumber(a.getString("number"))
            "open_sms" -> PhoneTools.openSms(a.getString("number"), a.optString("text", ""))
            "get_battery" -> PhoneTools.getBattery()
            "get_device_info" -> PhoneTools.getDeviceInfo()
            "toggle_flashlight" -> PhoneTools.toggleFlashlight()
            "set_volume" -> PhoneTools.setVolume(a.getString("type"), a.getInt("level"))
            "open_app" -> PhoneTools.openApp(a.optString("package", a.optString("packageName", "")))
            "share_text" -> PhoneTools.shareText(a.getString("text"))
            "create_project" -> ProjectsManager.create(a.getString("name"))
            "list_projects" -> ProjectsManager.list().joinToString("\n") { it.name }.ifBlank { "No projects." }
            "list_files" -> ProjectsManager.listFiles(a.getString("project"))
            "read_file" -> ProjectsManager.readFile(a.getString("project"), a.getString("path"))
            "write_file" -> ProjectsManager.writeFile(a.getString("project"), a.getString("path"), a.getString("content"))
            "delete_file" -> ProjectsManager.deleteFile(a.getString("project"), a.getString("path"))
            "http_get" -> httpGet(a.getString("url"))
            "scan_network" -> scanNetwork()
            "send_whatsapp" -> Messengers.sendWhatsApp(a.getString("text"))
            "trigger_apk_build" -> triggerApkBuild()
            else -> "Unknown tool: $name"
        }
    }

    private suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
        if (!Store.internetEnabled) return@withContext "Internet access is disabled (Settings -> Permissions)."
        Net.get(url).take(8000)
    }

    private suspend fun scanNetwork(): String {
        if (!Store.networkAccessEnabled) return "Network scan is disabled (Settings -> Permissions)."
        val local = localIp() ?: return "No local WiFi IP found."
        val prefix = local.substringBeforeLast(".")
        val found = coroutineScope {
            (1..254).map { i ->
                async(Dispatchers.IO) {
                    val ip = "$prefix.$i"
                    try {
                        if (InetAddress.getByName(ip).isReachable(300)) ip else null
                    } catch (_: Exception) { null }
                }
            }.awaitAll().filterNotNull()
        }
        return if (found.isEmpty()) "No devices found in $prefix.0/24."
        else "Own IP: $local\nDevices:\n${found.joinToString("\n")}"
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
            return@withContext "GitHub token or repository missing (Settings -> APK Builds)."
        }
        Net.postJson(
            "https://api.github.com/repos/$repo/actions/workflows/build-apk.yml/dispatches",
            org.json.JSONObject().put("ref", "main").toString(),
            mapOf("Authorization" to "Bearer $token", "Accept" to "application/vnd.github+json")
        )
        "APK build started! Progress: https://github.com/$repo/actions"
    }
}
