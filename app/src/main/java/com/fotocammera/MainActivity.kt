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
import com.fotocammera.databinding.ActivityMainBinding
import com.fotocammera.utils.PreferencesManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferencesManager: PreferencesManager
    
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        
        setupClickListeners()
        checkFirstLaunch()
    }
    
    private fun setupClickListeners() {
        binding.btnOpenCamera.setOnClickListener {
            requestPermissionsAndOpenCamera()
        }
        
        binding.btnSetDefault.setOnClickListener {
            showSetDefaultDialog()
        }
    }
    
    private fun checkFirstLaunch() {
        if (preferencesManager.isFirstLaunch()) {
            showSetDefaultDialog()
            preferencesManager.setFirstLaunchCompleted()
        }
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
        
        // Debug: Show which permissions are missing
        if (permissionsToRequest.isNotEmpty()) {
            val missingPermissions = permissionsToRequest.joinToString(", ") { 
                it.substringAfterLast(".") 
            }
            Toast.makeText(this, "Missing permissions: $missingPermissions", Toast.LENGTH_LONG).show()
        }
        
        if (permissionsToRequest.isEmpty()) {
            openCamera()
        } else {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    private fun openCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }
    
    private fun showSetDefaultDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.set_as_default_camera))
            .setMessage(getString(R.string.set_as_default_message))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                openDefaultAppSettings()
            }
            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton(getString(R.string.later)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun openDefaultAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                this,
                "Please find 'Default apps' and set FotoCamera as default camera",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(getString(R.string.permission_denied))
            .setPositiveButton("Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}