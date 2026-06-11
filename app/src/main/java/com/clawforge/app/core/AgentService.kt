package com.clawforge.app.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.clawforge.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Foreground-Service: hält den Agenten am Leben, solange das Handy an ist,
 * und beantwortet eingehende Telegram-Nachrichten per Long-Polling.
 */
class AgentService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var telegramJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        val channel = NotificationChannel(
            CHANNEL_ID, "ClawForge Agent", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ClawForge Agent aktiv")
            .setContentText("Die KI läuft im Hintergrund.")
            .setSmallIcon(R.drawable.ic_claw)
            .setOngoing(true)
            .build()
        startForeground(1, notif)
        startTelegramIfEnabled()
        return START_STICKY
    }

    private fun startTelegramIfEnabled() {
        telegramJob?.cancel()
        if (!Store.telegramEnabled || Store.telegramToken.isBlank()) return
        telegramJob = scope.launch {
            var offset = 0L
            while (isActive) {
                try {
                    val token = Store.telegramToken
                    val resp = Net.get(
                        "https://api.telegram.org/bot$token/getUpdates?timeout=50&offset=${offset + 1}"
                    )
                    val updates = JSONObject(resp).getJSONArray("result")
                    for (i in 0 until updates.length()) {
                        val u = updates.getJSONObject(i)
                        offset = maxOf(offset, u.getLong("update_id"))
                        val msg = u.optJSONObject("message") ?: continue
                        val text = msg.optString("text")
                        if (text.isBlank()) continue
                        val chatId = msg.getJSONObject("chat").getLong("id").toString()
                        val answer = try {
                            AgentEngine.run(listOf(ChatMsg("user", text)))
                        } catch (e: Exception) {
                            "Fehler: ${e.message}"
                        }
                        Messengers.sendTelegram(chatId, answer)
                    }
                } catch (_: Exception) {
                    delay(5000)
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "agent"

        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, AgentService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, AgentService::class.java))
        }
    }
}

/** Startet den Agenten nach einem Neustart des Handys automatisch. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Store.init(context)
            if (Store.alwaysOn) AgentService.start(context)
        }
    }
}
