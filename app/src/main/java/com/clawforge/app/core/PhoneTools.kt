package com.clawforge.app.core

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import com.clawforge.app.ClawForgeApp

object PhoneTools {
    private val ctx: Context get() = ClawForgeApp.ctx

    fun setAlarm(hour: Int, minute: Int, label: String = "ClawForge"): String = try {
        ctx.startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        "✅ Wecker für ${hour.toString().padStart(2,'0')}:${minute.toString().padStart(2,'0')} Uhr gestellt${if (label.isNotBlank()) " – \"$label\"" else ""}."
    } catch (e: Exception) { "❌ Wecker-Fehler: ${e.message}" }

    fun setTimer(seconds: Int, label: String = "Timer"): String = try {
        ctx.startActivity(Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        val m = seconds / 60; val s = seconds % 60
        "✅ Timer gestartet: ${if (m > 0) "${m}min " else ""}${if (s > 0) "${s}s" else ""}."
    } catch (e: Exception) { "❌ Timer-Fehler: ${e.message}" }

    fun openUrl(url: String): String = try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        "✅ Browser geöffnet: $url"
    } catch (e: Exception) { "❌ Fehler: ${e.message}" }

    fun dialNumber(number: String): String = try {
        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        "✅ Anruf-App geöffnet für $number."
    } catch (e: Exception) { "❌ Fehler: ${e.message}" }

    fun openSms(number: String, text: String): String = try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:$number")
            putExtra("sms_body", text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        "✅ SMS-App geöffnet für $number."
    } catch (e: Exception) { "❌ Fehler: ${e.message}" }

    fun getBattery(): String {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val lvl = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return "🔋 Akkustand: $lvl% ${if (bm.isCharging) "(lädt ⚡)" else "(nicht ladend)"}"
    }

    fun getDeviceInfo(): String = buildString {
        appendLine("📱 Gerät: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("🤖 Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        append("🔧 Kernel: ${System.getProperty("os.version")}")
    }

    fun vibrate(ms: Long = 500): String = try {
        if (Build.VERSION.SDK_INT >= 31) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            (ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                .vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        "✅ Vibration ausgelöst (${ms}ms)."
    } catch (e: Exception) { "❌ Fehler: ${e.message}" }

    private var torchOn = false
    fun toggleFlashlight(): String {
        return try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull() ?: return "❌ Keine Kamera gefunden."
            torchOn = !torchOn
            cm.setTorchMode(id, torchOn)
            if (torchOn) "✅ Taschenlampe AN 🔦" else "✅ Taschenlampe AUS"
        } catch (e: Exception) { "❌ Fehler: ${e.message}" }
    }

    fun setVolume(type: String, level: Int): String = try {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val stream = when (type.lowercase().trim()) {
            "musik", "music", "media" -> AudioManager.STREAM_MUSIC
            "klingelton", "ring", "anruf" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            "benachrichtigung", "notification" -> AudioManager.STREAM_NOTIFICATION
            else -> AudioManager.STREAM_MUSIC
        }
        val max = am.getStreamMaxVolume(stream)
        am.setStreamVolume(stream, (level.coerceIn(0, 100) * max / 100), 0)
        "✅ Lautstärke ($type) auf $level% gesetzt."
    } catch (e: Exception) { "❌ Fehler: ${e.message}" }

    fun openApp(packageName: String): String {
        return try {
            val intent = ctx.packageManager.getLaunchIntentForPackage(packageName)
                ?: return "❌ App '$packageName' nicht installiert."
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            "✅ $packageName geöffnet."
        } catch (e: Exception) { "❌ Fehler: ${e.message}" }
    }

    fun shareText(text: String): String = try {
        ctx.startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }, "Teilen via"
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        "✅ Teilen-Dialog geöffnet."
    } catch (e: Exception) { "❌ Fehler: ${e.message}" }
}
