package com.danielrosehill.familypager.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import com.danielrosehill.familypager.data.SettingsRepository
import com.danielrosehill.familypager.network.PushoverApi
import com.danielrosehill.familypager.notifications.NotificationHelper
import kotlinx.coroutines.*
import okhttp3.*

class PushoverListenerService : Service() {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder().build()
    private val api = PushoverApi()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var settings: SettingsRepository
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        notificationHelper = NotificationHelper(this)

        val notification = notificationHelper.buildServiceNotification()
        startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)

        acquireWakeLock()
        connectWebSocket()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "Service stopped")
        serviceScope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FamilyPager::ListenerWakeLock"
        ).apply { acquire(Long.MAX_VALUE) }
    }

    private fun connectWebSocket() {
        val secret = settings.sessionSecret
        val deviceId = settings.deviceId

        if (secret.isBlank() || deviceId.isBlank()) {
            stopSelf()
            return
        }

        val request = Request.Builder()
            .url("wss://client.pushover.net/push")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("login:$deviceId:$secret\n")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                when {
                    text.contains("!") -> onNewMessage()
                    text.contains("R") -> reconnect()
                    text.contains("E") -> {
                        // Permanent error — need re-auth
                        stopSelf()
                    }
                    text.contains("A") -> {
                        // Another device took over
                        stopSelf()
                    }
                    // '#' is keep-alive, ignore
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Reconnect after delay
                serviceScope.launch {
                    delay(5000)
                    connectWebSocket()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (code != 1000) {
                    serviceScope.launch {
                        delay(3000)
                        connectWebSocket()
                    }
                }
            }
        })
    }

    private fun reconnect() {
        webSocket?.close(1000, "Reconnecting")
        serviceScope.launch {
            delay(1000)
            connectWebSocket()
        }
    }

    private fun onNewMessage() {
        serviceScope.launch {
            val result = api.fetchMessages(settings.sessionSecret, settings.deviceId)
            result.onSuccess { messages ->
                var highestId = 0L
                for (msg in messages) {
                    notificationHelper.showPageNotification(
                        title = msg.title,
                        message = msg.message,
                        priority = msg.priority
                    )
                    if (msg.id > highestId) highestId = msg.id
                }
                if (highestId > 0) {
                    api.deleteMessages(settings.sessionSecret, settings.deviceId, highestId)
                }
            }
        }
    }
}
