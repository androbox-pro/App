package com.kashem.shaikh

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var requiredPermissions: Array<String>
    private var isServiceStarted = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permission granted -> start services & then ask for MediaProjection
            startServices()
            requestScreenRecordPermission()
        } else {
            Toast.makeText(this, "Some permissions denied. App may not work properly.", Toast.LENGTH_LONG).show()
        }
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            ForegroundService.setMediaProjectionData(result.resultCode, result.data!!)
            ScreenRecorder.setMediaProjection(result.resultCode, result.data!!)
            Toast.makeText(this, "✅ Screen record permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "❌ Screen record permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, insets.getInsets(WindowInsetsCompat.Type.systemBars()).right, insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            insets
        }

        // Prepare permission list
        val permissionsList = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
            permissionsList.add(Manifest.permission.FOREGROUND_SERVICE_CAMERA)
            permissionsList.add(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION)
            permissionsList.add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
        }
        requiredPermissions = permissionsList.toTypedArray()

        if (hasPermissions()) {
            // All permissions already granted
            startServices()
            requestScreenRecordPermission()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    private fun hasPermissions() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startServices() {
        if (!isServiceStarted) {
            startService(Intent(this, ForegroundService::class.java))
            startService(Intent(this, ScreenRecorder::class.java))
            isServiceStarted = true
        }
    }

    fun requestScreenRecordPermission() {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }
}