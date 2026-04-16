package com.kashem.shaikh

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent

class CommandProcessor(private val context: Context) {

    fun execute(command: String, accessibilityService: AccessibilityService?): Boolean {
        return when {
            command.startsWith("open ") -> {
                val appName = command.substringAfter("open ").trim()
                openAppByName(appName)
                true
            }
            command == "back" || command == "go back" -> {
                accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                true
            }
            command == "home" -> {
                accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                true
            }
            command == "recent" -> {
                accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                true
            }
            command == "scroll down" -> {
                // সঠিক কনস্ট্যান্ট ব্যবহার (API 23+)
                accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_SCROLL_DOWN)
                true
            }
            command == "scroll up" -> {
                accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_SCROLL_UP)
                true
            }
            else -> false
        }
    }

    private fun openAppByName(name: String) {
        val packageName = when (name) {
            "gmail" -> "com.google.android.gm"
            "settings" -> "com.android.settings"
            "camera" -> "com.android.camera"
            "chrome" -> "com.android.chrome"
            "whatsapp" -> "com.whatsapp"
            "youtube" -> "com.google.android.youtube"
            else -> null
        }
        packageName?.let {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(it)
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}