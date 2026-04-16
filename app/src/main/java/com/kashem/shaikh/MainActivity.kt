package com.kashem.shaikh

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var btnEnableService: Button
    private lateinit var btnStartService: Button

    companion object {
        private const val REQUEST_RECORD_AUDIO = 100
        private const val REQUEST_OVERLAY_PERMISSION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        statusText = findViewById(R.id.statusText)
        btnEnableService = findViewById(R.id.btnEnableService)
        btnStartService = findViewById(R.id.btnStartService)

        checkAccessibilityStatus()

        btnEnableService.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnStartService.setOnClickListener {
            requestPermissionsAndStart()
        }
    }

    private fun checkAccessibilityStatus() {
        val isEnabled = isAccessibilityServiceEnabled()
        if (isEnabled) {
            statusText.text = "Status: Voice Access Service is ON"
            btnEnableService.isEnabled = false
        } else {
            statusText.text = "Status: Please enable Accessibility Service"
            btnEnableService.isEnabled = true
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = ComponentName(this, VoiceAccessService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service.flattenToString()) == true
    }

    private fun requestPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE),
                    102
                )
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            return
        }

        startVoiceAccessService()
    }

    private fun startVoiceAccessService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(this, ForegroundService::class.java)
            ContextCompat.startForegroundService(this, intent)
        }
        Toast.makeText(this, "Voice Access is running in background", Toast.LENGTH_LONG).show()
        statusText.text = "Status: Voice Access Active (Service must be enabled in Accessibility)"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestPermissionsAndStart()
                } else {
                    Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAccessibilityStatus()
    }
}