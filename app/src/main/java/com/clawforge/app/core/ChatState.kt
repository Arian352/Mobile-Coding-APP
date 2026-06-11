package com.clawforge.app.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Gemeinsamer Chat-Verlauf für die Oberfläche. */
object ChatState {
    val messages = mutableStateListOf<ChatMsg>(
        ChatMsg(
            "assistant",
            "👋 Hey! Ich bin ClawForge – deine KI auf diesem Handy.\n\n" +
                "Trage zuerst in den Einstellungen deinen API-Schlüssel ein (z. B. Gemini) " +
                "und wähle ein Modell. Danach kann ich Projekte, Webseiten, Dateien und " +
                "sogar APK-Builds für dich erstellen. ⚡"
        )
    )
    var busy by mutableStateOf(false)
}
