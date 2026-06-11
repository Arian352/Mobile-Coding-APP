package com.clawforge.app.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class PermEntry(
    val emoji: String,
    val name: String,
    val permission: String,
    val minSdk: Int = 0,
    val maxSdk: Int = Int.MAX_VALUE
)

private val ALL_PERMS = listOf(
    PermEntry("📷", "Kamera", Manifest.permission.CAMERA),
    PermEntry("🎤", "Mikrofon", Manifest.permission.RECORD_AUDIO),
    PermEntry("📍", "Standort (genau)", Manifest.permission.ACCESS_FINE_LOCATION),
    PermEntry("🗺️", "Standort (grob)", Manifest.permission.ACCESS_COARSE_LOCATION),
    PermEntry("🌍", "Hintergrund-GPS", Manifest.permission.ACCESS_BACKGROUND_LOCATION),
    PermEntry("📞", "Anrufe tätigen", Manifest.permission.CALL_PHONE),
    PermEntry("📲", "Anruf annehmen", Manifest.permission.ANSWER_PHONE_CALLS, minSdk = 26),
    PermEntry("📤", "Ausgehende Anrufe", Manifest.permission.PROCESS_OUTGOING_CALLS, maxSdk = 29),
    PermEntry("💬", "SMS senden", Manifest.permission.SEND_SMS),
    PermEntry("📩", "SMS lesen", Manifest.permission.READ_SMS),
    PermEntry("📥", "SMS empfangen", Manifest.permission.RECEIVE_SMS),
    PermEntry("👥", "Kontakte lesen", Manifest.permission.READ_CONTACTS),
    PermEntry("✏️", "Kontakte schreiben", Manifest.permission.WRITE_CONTACTS),
    PermEntry("📋", "Anruf-Verlauf", Manifest.permission.READ_CALL_LOG),
    PermEntry("🗒️", "Anruf-Log schreiben", Manifest.permission.WRITE_CALL_LOG),
    PermEntry("📱", "Telefon-Status", Manifest.permission.READ_PHONE_STATE),
    PermEntry("🔢", "Telefon-Nummer", Manifest.permission.READ_PHONE_NUMBERS, minSdk = 26),
    PermEntry("🔔", "Benachrichtigungen", Manifest.permission.POST_NOTIFICATIONS, minSdk = 33),
    PermEntry("🖼️", "Bilder", Manifest.permission.READ_MEDIA_IMAGES, minSdk = 33),
    PermEntry("🎬", "Videos", Manifest.permission.READ_MEDIA_VIDEO, minSdk = 33),
    PermEntry("🎵", "Musik", Manifest.permission.READ_MEDIA_AUDIO, minSdk = 33),
    PermEntry("📁", "Dateien lesen", Manifest.permission.READ_EXTERNAL_STORAGE, maxSdk = 32),
    PermEntry("💾", "Dateien schreiben", Manifest.permission.WRITE_EXTERNAL_STORAGE, maxSdk = 29),
    PermEntry("🔵", "Bluetooth", Manifest.permission.BLUETOOTH_CONNECT, minSdk = 31),
    PermEntry("🔍", "BT-Scan", Manifest.permission.BLUETOOTH_SCAN, minSdk = 31),
    PermEntry("❤️", "Körper-Sensor", Manifest.permission.BODY_SENSORS),
    PermEntry("🏃", "Aktivitäten", Manifest.permission.ACTIVITY_RECOGNITION, minSdk = 29),
)

fun allRuntimePermissions(): Array<String> {
    val sdk = Build.VERSION.SDK_INT
    return ALL_PERMS
        .filter { sdk >= it.minSdk && sdk <= it.maxSdk }
        .map { it.permission }
        .toTypedArray()
}

@Composable
fun PermissionScreen(onRequest: (Array<String>) -> Unit) {
    val sdk = Build.VERSION.SDK_INT
    val perms = ALL_PERMS.filter { sdk >= it.minSdk && sdk <= it.maxSdk }

    Box(
        Modifier.fillMaxSize().background(BgDeep)
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Column(
                Modifier.fillMaxWidth().background(BgDeep).padding(top = 52.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(80.dp).clip(RoundedCornerShape(24.dp))
                        .background(GradientBrand2).padding(18.dp),
                    contentAlignment = Alignment.Center
                ) { Text("⚡", fontSize = 38.sp) }
                Spacer(Modifier.height(16.dp))
                Text(
                    "ClawForge",
                    style = TextStyle(brush = GradientBrand, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Damit die KI alles auf deinem Handy\nerledigen kann, braucht sie Zugriff.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            // Permission grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(perms) { p ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BgCard)
                            .border(0.7.dp, BorderColor, RoundedCornerShape(14.dp))
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(38.dp).clip(CircleShape)
                                .background(BrandIndigo.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) { Text(p.emoji, fontSize = 18.sp) }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            p.name,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Allow button
            Column(
                Modifier.fillMaxWidth().background(BgDeep)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(GradientBrand2)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onRequest(allRuntimePermissions()) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Alle Berechtigungen erlauben",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Du kannst Berechtigungen jederzeit in den Android-Einstellungen widerrufen.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
