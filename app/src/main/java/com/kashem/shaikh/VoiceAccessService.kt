package com.kashem.shaikh

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.widget.Toast

class VoiceAccessService : AccessibilityService() {

    private lateinit var speechManager: SpeechRecognizerManager
    private lateinit var commandProcessor: CommandProcessor

    companion object {
        var instance: VoiceAccessService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        commandProcessor = CommandProcessor(this)
        speechManager = SpeechRecognizerManager(this) { command ->
            Log.d("VoiceAccess", "Recognized: $command")
            val executed = commandProcessor.execute(command, this)
            if (!executed) {
                Toast.makeText(this, "Unknown command: $command", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        speechManager.initialize()
        speechManager.startListening()
        Toast.makeText(this, "Voice Access Started", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // প্রয়োজনে ইভেন্ট হ্যান্ডেল করুন
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        speechManager.destroy()
        instance = null
        Toast.makeText(this, "Voice Access Stopped", Toast.LENGTH_SHORT).show()
    }
}