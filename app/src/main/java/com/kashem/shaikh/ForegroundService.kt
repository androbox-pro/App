package com.kashem.shaikh

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class ForegroundService : Service() {

    private val channelId = "ForegroundServiceChannel"
    private val notificationId = 1
    private lateinit var webSocketManager: WebSocketManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
        initWebSocket()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, ForegroundService::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Device Controller")
            .setContentText("Starting...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(notificationId, notification)
    }

    private fun initWebSocket() {
        webSocketManager = WebSocketManager(
            context = applicationContext,
            onMessage = { message ->
                serviceScope.launch {
                    CommandProcessor.processCommand(message, applicationContext)
                }
            },
            onConnectionChange = { isConnected ->
                updateNotification(if (isConnected) "Connected" else "Disconnected")
            }
        )
        webSocketManager.connect()
    }

    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Device Controller")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        webSocketManager.disconnect()
    }
}