# ⚡ ClawForge

**ClawForge** ist deine eigene KI als Android-App – inspiriert von „Open Claw".
Die KI läuft direkt auf deinem Handy, wird über einen API-Schlüssel aktiviert
und kann Projekte, Webseiten, Dateien und sogar APK-Builds für dich erstellen.

## 📲 APK herunterladen

Die APK wird automatisch von GitHub Actions gebaut:

1. Gehe zu **Actions** → neuester „Build APK"-Lauf → Artefakt **ClawForge-APK** herunterladen.
2. Oder (sobald der Branch in `main` ist): unter **Releases** die fertige APK laden.
3. APK auf dem Handy installieren („Unbekannte Quellen" erlauben).

## ✨ Funktionen

| Funktion | Beschreibung |
|---|---|
| 🤖 KI per API | Gemini, OpenAI, Anthropic oder OpenRouter – API-Key eintragen, fertig |
| 🎛️ Modell-Auswahl | Bei jedem Anbieter frei wählbar (z. B. bei Gemini: 2.5 Pro, 2.5 Flash …) |
| 📁 Projekte | Alle von der KI erstellten Projekte und Dateien in der App ansehen |
| 📜 Regel-Ordner | Eigene Regeln mit Prioritäten (kleinere Nummer = wichtiger), Beispiel inklusive |
| 💬 Telegram | Eigener Telegram-Bot: Schreib deiner KI von überall |
| 📱 WhatsApp | Die KI kann dir Nachrichten per WhatsApp Cloud API senden |
| 🌐 Internet | Die KI darf Webseiten und APIs abrufen (abschaltbar) |
| 📡 Netzwerk-Scan | Findet Geräte in deinem WLAN – standardmäßig **aus**, erst aktivieren |
| 🔋 Always-On | Foreground-Service: Die KI läuft, solange das Handy an ist (auch nach Neustart) |
| 📦 APK-Builds | Die KI kann per GitHub Actions APK-Builds starten |
| 🌍 Webseiten | Die KI erstellt komplette Webseiten (HTML/CSS/JS) als Projekt |

## 🚀 Einrichtung

1. App öffnen → Tab **Setup**.
2. Anbieter wählen (z. B. **Gemini**) und API-Schlüssel eintragen
   (Gemini-Key gratis unter [aistudio.google.com](https://aistudio.google.com/apikey)).
3. Modell wählen oder eigenes eintragen.
4. Optional: **Agent immer aktiv** einschalten.

### Telegram-Bot

1. In Telegram [@BotFather](https://t.me/BotFather) öffnen → `/newbot` → Token kopieren.
2. Token in ClawForge unter **Setup → Telegram** eintragen und aktivieren.
3. Deinem Bot schreiben – die KI antwortet automatisch.

### WhatsApp

WhatsApp nutzt die offizielle **Meta Cloud API** (Access-Token + Telefonnummer-ID
unter [developers.facebook.com](https://developers.facebook.com) anlegen).
Damit kann dir die KI Nachrichten senden. Für *eingehende* WhatsApp-Nachrichten
verlangt Meta einen Webhook-Server – nutze dafür am besten den Telegram-Bot,
der ohne Server funktioniert.

### Regeln

Im Tab **Regeln** liegt der Regel-Ordner. Jede Regel ist eine Markdown-Datei,
die Nummer am Anfang des Dateinamens bestimmt die Priorität (010 vor 020).
Ein Beispiel („Du bist ein KI-Programmier-Assistent …" mit Abschnitten wie
*Verhalten*, *Regeln*, *Prioritäten*) ist schon enthalten und kann bearbeitet werden.

### APK-Builds durch die KI

1. GitHub-Token erstellen (Scope `repo`/`actions`).
2. Unter **Setup → APK-Builds** Token und Repository (`benutzer/repo`) eintragen.
3. Im Chat sagen: „Starte einen APK-Build" – die KI stößt den GitHub-Actions-Workflow an.

## 🔒 Sicherheit

- **Netzwerk-Zugriff** (Geräte im WLAN scannen) ist standardmäßig **deaktiviert**
  und kann jederzeit ein-/ausgeschaltet werden.
- **Internet-Zugriff** der KI ist ebenfalls abschaltbar.
- API-Schlüssel bleiben lokal auf dem Handy gespeichert.

## 🛠️ Selbst bauen

```bash
gradle assembleRelease
# APK liegt danach in app/build/outputs/apk/release/
```

Benötigt: JDK 17, Android SDK 34. Oder einfach den GitHub-Actions-Workflow nutzen.
