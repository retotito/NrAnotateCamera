package com.fotocammera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.CheckBox
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
            // First launch - show the default camera dialog
            showSetDefaultDialog()
            preferencesManager.setFirstLaunchCompleted()
        } else if (!preferencesManager.isDontAskAgainDefault()) {
            // Not first launch, but user hasn't checked "don't ask again"
            showSetDefaultDialog()
        } else {
            // User has checked "don't ask again" - go directly to camera
            requestPermissionsAndOpenCamera()
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
        // Inflate custom layout with checkbox
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_default_camera, null)
        val checkBox = dialogView.findViewById<CheckBox>(R.id.cbDontAskAgain)
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.set_as_default_camera))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                // Save the checkbox state
                if (checkBox.isChecked) {
                    preferencesManager.setDontAskAgainDefault(true)
                }
                openDefaultAppSettings()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                // Save the checkbox state
                if (checkBox.isChecked) {
                    preferencesManager.setDontAskAgainDefault(true)
                }
                // Go directly to camera
                requestPermissionsAndOpenCamera()
                dialog.dismiss()
            }
            .setNeutralButton(getString(R.string.later)) { dialog, _ ->
                // Don't save checkbox state for "Later" - ask again next time
                // Go directly to camera
                requestPermissionsAndOpenCamera()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun openDefaultAppSettings() {
        // Try multiple approaches to help user set as default camera
        showDefaultAppInstructions()
    }
    
    private fun showDefaultAppInstructions() {
        val instructions = """
To set FotoCamera as your default camera app:

EASIEST METHOD - Test Camera Choice:
• Tap "Test Camera Choice" below
• Select "FotoCamera" 
• Tap "Always" (NOT "Just once")

SETTINGS METHOD (varies by phone):
Samsung: Settings → Apps → Choose default apps → Camera app
Google/Pixel: Settings → Apps → Default apps → Camera app  
OnePlus: Settings → Apps → App management → Default app settings → Camera
Xiaomi/MIUI: Settings → Manage apps → Permissions → Camera → Choose default
Huawei: Settings → Apps → Default apps → Camera
General: Settings → Apps → Default apps → Camera

ALTERNATIVE METHOD:
• Open any app that needs camera (WhatsApp, Instagram)
• When it asks for camera permission, choose "FotoCamera"
• Select "Always" instead of "Just once"
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("How to Set as Default Camera")
            .setMessage(instructions)
            .setPositiveButton("Open Settings") { _, _ ->
                tryOpenDefaultAppSettings()
            }
            .setNegativeButton("Got it") { dialog, _ ->
                dialog.dismiss()
                // Still open camera so user can use the app
                requestPermissionsAndOpenCamera()
            }
            .setNeutralButton("Test Camera Choice") { _, _ ->
                testCameraAppSelection()
            }
            .show()
    }
    
    private fun tryOpenDefaultAppSettings() {
        // Try different settings screens in order of preference
        val settingsIntents = listOf(
            // Try direct default apps first
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            // Try application details for this app
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            },
            // Try general app settings
            Intent(Settings.ACTION_APPLICATION_SETTINGS),
            // Fallback to main settings
            Intent(Settings.ACTION_SETTINGS)
        )
        
        var settingsOpened = false
        for (intent in settingsIntents) {
            try {
                startActivity(intent)
                settingsOpened = true
                showSettingsGuidance()
                break
            } catch (e: Exception) {
                // Continue to next option
                continue
            }
        }
        
        if (!settingsOpened) {
            Toast.makeText(
                this,
                "Please manually open Settings and look for 'Default apps' or 'App preferences'",
                Toast.LENGTH_LONG
            ).show()
            // Still open camera
            requestPermissionsAndOpenCamera()
        }
    }
    
    private fun showSettingsGuidance() {
        Toast.makeText(
            this,
            "Look for: Default apps → Camera, or Apps → FotoCamera → Open by default",
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun copyInstructionsToClipboard(instructions: String) {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Default Camera Instructions", instructions)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Instructions copied to clipboard!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not copy to clipboard", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun testCameraAppSelection() {
        try {
            // This will trigger the system to show camera app selection dialog
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            // Don't set any specific app - let the system show all camera apps
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(intent, "Choose Camera App (Select FotoCamera and tap 'Always')"))
                Toast.makeText(
                    this,
                    "Select 'FotoCamera' and tap 'Always' to set as default!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                // Fallback to our camera
                requestPermissionsAndOpenCamera()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not test camera selection", Toast.LENGTH_SHORT).show()
            requestPermissionsAndOpenCamera()
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