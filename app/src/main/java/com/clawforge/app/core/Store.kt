package com.clawforge.app.core

import android.content.Context
import android.content.SharedPreferences
import java.io.File

/** Zentrale Einstellungen der App (SharedPreferences). */
object Store {
    private lateinit var prefs: SharedPreferences

    fun init(ctx: Context) {
        if (::prefs.isInitialized) return
        prefs = ctx.getSharedPreferences("clawforge", Context.MODE_PRIVATE)
        RulesManager.init(ctx)
        ProjectsManager.init(ctx)
    }

    private fun str(key: String, def: String = "") = prefs.getString(key, def) ?: def
    private fun put(key: String, v: String) = prefs.edit().putString(key, v).apply()
    private fun bool(key: String, def: Boolean = false) = prefs.getBoolean(key, def)
    private fun put(key: String, v: Boolean) = prefs.edit().putBoolean(key, v).apply()

    var provider: String
        get() = str("provider", "Anthropic")
        set(v) = put("provider", v)

    fun apiKey(provider: String) = str("apikey_$provider")
    fun setApiKey(provider: String, v: String) = put("apikey_$provider", v)

    fun model(provider: String): String {
        val m = str("model_$provider")
        return m.ifBlank { AiClient.defaultModels(provider).first() }
    }

    fun setModel(provider: String, v: String) = put("model_$provider", v)

    var internetEnabled: Boolean
        get() = bool("internet", true)
        set(v) = put("internet", v)

    var networkAccessEnabled: Boolean
        get() = bool("netaccess", false)
        set(v) = put("netaccess", v)

    var alwaysOn: Boolean
        get() = bool("alwayson", false)
        set(v) = put("alwayson", v)

    var telegramEnabled: Boolean
        get() = bool("tg_enabled", false)
        set(v) = put("tg_enabled", v)

    var telegramToken: String
        get() = str("tg_token")
        set(v) = put("tg_token", v)

    var whatsappEnabled: Boolean
        get() = bool("wa_enabled", false)
        set(v) = put("wa_enabled", v)

    var whatsappToken: String
        get() = str("wa_token")
        set(v) = put("wa_token", v)

    var whatsappPhoneId: String
        get() = str("wa_phone_id")
        set(v) = put("wa_phone_id", v)

    var whatsappTo: String
        get() = str("wa_to")
        set(v) = put("wa_to", v)

    var githubToken: String
        get() = str("gh_token")
        set(v) = put("gh_token", v)

    var githubRepo: String
        get() = str("gh_repo")
        set(v) = put("gh_repo", v)
}

/**
 * Verwaltet den Regel-Ordner. Regeln sind Markdown-Dateien; eine kleinere
 * Nummer am Anfang des Dateinamens bedeutet höhere Priorität.
 */
object RulesManager {
    lateinit var dir: File
        private set

    private val EXAMPLE = """
# Verhalten

Du bist ein KI-Programmier-Assistent in der App ClawForge.
Du hilfst beim Erstellen von Apps, Webseiten, Skripten und Dateien
und führst Aufgaben mit deinen Werkzeugen wirklich aus.

---

# Regeln

1. Antworte klar, freundlich und auf Deutsch (außer der Nutzer schreibt in einer anderen Sprache).
2. Schreibe sauberen, gut kommentierten Code.
3. Frage nach, wenn eine Aufgabe unklar ist.
4. Lege für jede neue Aufgabe ein eigenes Projekt an und speichere Dateien dort.

---

# Prioritäten

- Sicherheit geht vor Geschwindigkeit.
- Regeln in Dateien mit kleinerer Nummer haben höhere Priorität.
""".trimIndent()

    fun init(ctx: Context) {
        dir = File(ctx.filesDir, "rules")
        if (!dir.exists()) {
            dir.mkdirs()
            File(dir, "010_assistent.md").writeText(EXAMPLE)
        }
    }

    fun list(): List<File> = (dir.listFiles() ?: emptyArray()).filter { it.isFile }.sortedBy { it.name }

    fun create(name: String, content: String): File {
        val safe = name.replace(Regex("[^A-Za-z0-9_\\-.]"), "_")
        val file = File(dir, if (safe.endsWith(".md")) safe else "$safe.md")
        file.writeText(content)
        return file
    }

    fun combined(): String =
        list().joinToString("\n\n---\n\n") { "### ${it.name}\n${it.readText()}" }
            .ifBlank { "(keine Regeln definiert)" }
}

/** Verwaltet den Projekte-Ordner, in dem die KI Dateien anlegt. */
object ProjectsManager {
    lateinit var dir: File
        private set

    fun init(ctx: Context) {
        dir = File(ctx.filesDir, "projects")
        dir.mkdirs()
    }

    fun list(): List<File> = (dir.listFiles() ?: emptyArray()).filter { it.isDirectory }.sortedBy { it.name }

    fun create(name: String): String {
        val safe = name.replace(Regex("[^A-Za-z0-9_\\-. ]"), "_").trim()
        if (safe.isBlank()) return "Ungültiger Projektname."
        val p = File(dir, safe)
        return if (p.exists()) "Projekt '$safe' existiert bereits."
        else if (p.mkdirs()) "Projekt '$safe' wurde angelegt."
        else "Projekt '$safe' konnte nicht angelegt werden."
    }

    private fun resolve(project: String, path: String): File {
        val root = File(dir, project)
        if (!root.exists()) throw IllegalArgumentException("Projekt '$project' existiert nicht.")
        val f = File(root, path)
        if (!f.canonicalPath.startsWith(root.canonicalPath)) {
            throw IllegalArgumentException("Ungültiger Pfad: $path")
        }
        return f
    }

    fun fileList(project: String): List<String> {
        val root = File(dir, project)
        if (!root.exists()) return emptyList()
        return root.walkTopDown().filter { it.isFile }
            .map { it.relativeTo(root).path }.sorted().toList()
    }

    fun listFiles(project: String): String =
        fileList(project).joinToString("\n").ifBlank { "Projekt ist leer oder existiert nicht." }

    fun readFile(project: String, path: String): String {
        val f = resolve(project, path)
        if (!f.exists()) return "Datei nicht gefunden: $path"
        return f.readText()
    }

    fun writeFile(project: String, path: String, content: String): String {
        val f = resolve(project, path)
        f.parentFile?.mkdirs()
        f.writeText(content)
        return "Gespeichert: $project/$path (${content.length} Zeichen)"
    }

    fun deleteFile(project: String, path: String): String {
        val f = resolve(project, path)
        return if (f.delete()) "Gelöscht: $project/$path" else "Konnte nicht löschen: $path"
    }
}
