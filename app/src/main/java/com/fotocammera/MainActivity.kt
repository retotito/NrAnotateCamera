package com.fotocammera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    
    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        
        // Check storage permission based on Android version
        val storageGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
        } else {
            val writeGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
            val readGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            writeGranted && readGranted
        }
        
        if (cameraGranted && storageGranted) {
            openCamera()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check permissions and go straight to camera
        requestPermissionsAndOpenCamera()
    }
    
    private fun requestPermissionsAndOpenCamera() {
        val requiredPermissions = mutableListOf<String>().apply {
            add(Manifest.permission.CAMERA)
            
            // Different storage permissions based on Android version
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isEmpty()) {
            // All permissions already granted
            openCamera()
        } else {
            // Request missing permissions
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    private fun openCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivity so back button from camera goes to home screen
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Camera and storage permissions are required for this app to work properly.")
            .setPositiveButton("Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
        finish()
    }
}