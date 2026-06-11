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
import androidx.core.content.FileProvider
import com.clawforge.app.ClawForgeApp
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

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
        "Alarm set for ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}${if (label.isNotBlank()) " - \"$label\"" else ""}."
    } catch (e: Exception) { "Alarm error: ${e.message}" }

    fun setTimer(seconds: Int, label: String = "Timer"): String = try {
        ctx.startActivity(Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        val m = seconds / 60; val s = seconds % 60
        "Timer started: ${if (m > 0) "${m}min " else ""}${if (s > 0) "${s}s" else ""}."
    } catch (e: Exception) { "Timer error: ${e.message}" }

    fun openUrl(url: String): String = try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        "Browser opened: $url"
    } catch (e: Exception) { "Error: ${e.message}" }

    fun dialNumber(number: String): String = try {
        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        "Dialer opened for $number."
    } catch (e: Exception) { "Error: ${e.message}" }

    fun openSms(number: String, text: String): String = try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:$number")
            putExtra("sms_body", text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        "SMS app opened for $number."
    } catch (e: Exception) { "Error: ${e.message}" }

    fun getBattery(): String {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val lvl = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return "Battery: $lvl% ${if (bm.isCharging) "(charging)" else "(not charging)"}"
    }

    fun getDeviceInfo(): String = buildString {
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        append("Kernel: ${System.getProperty("os.version")}")
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
        "Vibration triggered (${ms}ms)."
    } catch (e: Exception) { "Error: ${e.message}" }

    private var torchOn = false
    fun toggleFlashlight(): String {
        return try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull() ?: return "No camera found."
            torchOn = !torchOn
            cm.setTorchMode(id, torchOn)
            if (torchOn) "Flashlight ON" else "Flashlight OFF"
        } catch (e: Exception) { "Error: ${e.message}" }
    }

    fun setVolume(type: String, level: Int): String = try {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val stream = when (type.lowercase().trim()) {
            "musik", "music", "media" -> AudioManager.STREAM_MUSIC
            "klingelton", "ring", "anruf", "ringtone" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            "benachrichtigung", "notification" -> AudioManager.STREAM_NOTIFICATION
            else -> AudioManager.STREAM_MUSIC
        }
        val max = am.getStreamMaxVolume(stream)
        am.setStreamVolume(stream, (level.coerceIn(0, 100) * max / 100), 0)
        "Volume ($type) set to $level%."
    } catch (e: Exception) { "Error: ${e.message}" }

    fun openApp(packageName: String): String {
        return try {
            val intent = ctx.packageManager.getLaunchIntentForPackage(packageName)
                ?: return "App '$packageName' not installed."
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            "$packageName opened."
        } catch (e: Exception) { "Error: ${e.message}" }
    }

    fun shareText(text: String): String = try {
        ctx.startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }, "Share via"
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        "Share dialog opened."
    } catch (e: Exception) { "Error: ${e.message}" }

    fun updateApk(downloadUrl: String): String {
        return try {
            val http = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(downloadUrl).build()
            val apkDir = File(ctx.cacheDir, "apk_downloads")
            apkDir.mkdirs()
            val apkFile = File(apkDir, "update.apk")
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return "Download failed: HTTP ${response.code}"
                }
                val body = response.body ?: return "Download failed: empty response body"
                apkFile.outputStream().use { out ->
                    body.byteStream().copyTo(out)
                }
            }
            val uri = FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(intent)
            "APK downloaded and installer launched. File: ${apkFile.absolutePath}"
        } catch (e: Exception) { "APK update error: ${e.message}" }
    }
}
