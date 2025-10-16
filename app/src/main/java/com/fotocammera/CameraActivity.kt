package com.fotocammera

import android.content.ContentValues
import android.content.res.Configuration
import android.graphics.*
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.fotocammera.databinding.ActivityCameraBinding
import com.fotocammera.utils.PreferencesManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCameraBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var cameraExecutor: ExecutorService
    
    private var imageCapture: ImageCapture? = null
    private var currentDisplayNumber: String = "0000"
    
    companion object {
        private const val TAG = "CameraActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        currentDisplayNumber = preferencesManager.getDisplayNumber()
        
        updateNumberDisplay()
        setupClickListeners()
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Update target rotation for image capture when orientation changes
        imageCapture?.targetRotation = binding.previewView.display.rotation
        Log.d(TAG, "Configuration changed to: ${if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) "Landscape" else "Portrait"}")
        Log.d(TAG, "Updated target rotation to: ${binding.previewView.display.rotation}")
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnCapture.setOnClickListener {
            takePhoto()
        }
        
        binding.llNumberDisplay.setOnClickListener {
            showNumberPickerDialog()
        }
    }
    
    private fun updateNumberDisplay() {
        val firstTwo = currentDisplayNumber.substring(0, 2)
        val lastTwo = currentDisplayNumber.substring(2, 4)
        
        binding.tvNumberFirst.text = firstTwo
        binding.tvNumberLast.text = lastTwo
    }
    
    private fun showNumberPickerDialog() {
        val dialog = NumberPickerDialog(this, currentDisplayNumber) { newNumber ->
            currentDisplayNumber = newNumber
            preferencesManager.saveDisplayNumber(newNumber)
            updateNumberDisplay()
        }
        dialog.show()
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            // Preview
            val preview = Preview.Builder()
                .setTargetRotation(binding.previewView.display.rotation)
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
            
            // ImageCapture with proper rotation handling
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(binding.previewView.display.rotation)
                .build()
            
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        // Show loading overlay to block user interactions
        showLoadingOverlay()
        
        // Capture current orientation for overlay positioning
        val currentOrientation = resources.configuration.orientation
        
        // Create time stamped name and MediaStore entry
        val name = generateFileName()
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Camera")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()
        
        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    hideLoadingOverlay()
                    Toast.makeText(
                        this@CameraActivity,
                        getString(R.string.photo_save_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Post-process the image to add number overlay
                    output.savedUri?.let { uri ->
                        addNumberOverlayToImage(uri, currentOrientation)
                        
                        // Clear IS_PENDING flag for Android 10+
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            val pendingContentValues = ContentValues().apply {
                                put(MediaStore.Images.Media.IS_PENDING, 0)
                            }
                            contentResolver.update(uri, pendingContentValues, null, null)
                        }
                        
                        // Notify media scanner so image appears in gallery apps
                        notifyMediaScanner(uri)
                    }
                    
                    hideLoadingOverlay()
                    val msg = "${getString(R.string.photo_saved)}: ${generateFileName()}"
                    Toast.makeText(this@CameraActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        )
    }
    
    private fun generateFileName(): String {
        val dateFormat = SimpleDateFormat("dd.MM-HH.mm.ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "$currentDisplayNumber-$timestamp.jpg"
    }
    
    private fun addNumberOverlayToImage(imageUri: android.net.Uri, deviceOrientation: Int) {
        try {
            // Read the original image
            val inputStream = contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap != null) {
                val imageWidth = originalBitmap.width
                val imageHeight = originalBitmap.height
                val isImageLandscape = imageWidth > imageHeight
                val isDeviceLandscape = deviceOrientation == Configuration.ORIENTATION_LANDSCAPE
                
                Log.d(TAG, "Image dimensions: ${imageWidth} x ${imageHeight} (${if (isImageLandscape) "landscape" else "portrait"})")
                Log.d(TAG, "Device orientation: ${if (isDeviceLandscape) "landscape" else "portrait"}")
                
                // Create a mutable copy
                val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutableBitmap)
                
                // Calculate overlay size - further reduced by 35% from previous size
                val scaleFactor = minOf(imageWidth, imageHeight) / 1000f
                val baseWidth = (616 * 0.6 * 0.65).toInt()  // 40% + 35% reduction: 616 * 0.6 * 0.65 = 240
                val baseHeight = (328 * 0.6 * 0.65).toInt() // 40% + 35% reduction: 328 * 0.6 * 0.65 = 128
                val numberWidth = (baseWidth * scaleFactor).toInt().coerceAtLeast(120)  // Further reduced minimum
                val numberHeight = (baseHeight * scaleFactor).toInt().coerceAtLeast(60) // Further reduced minimum
                
                val margin = 20 // Small margin from edges
                
                // Always position in bottom-right of the actual saved image
                val x = imageWidth - numberWidth - margin
                val y = imageHeight - numberHeight - margin
                
                Log.d(TAG, "Overlay position: ($x, $y), size: ${numberWidth}x${numberHeight}")
                Log.d(TAG, "Device vs Image orientation match: ${isDeviceLandscape == isImageLandscape}")
                
                // Draw number background and text
                drawNumberOverlay(canvas, x.toFloat(), y.toFloat(), numberWidth, numberHeight)
                
                // Save the modified image back
                val outputStream = contentResolver.openOutputStream(imageUri)
                outputStream?.use {
                    mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                }
                
                originalBitmap.recycle()
                mutableBitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay to image", e)
        }
    }
    
    private fun drawNumberOverlay(canvas: Canvas, x: Float, y: Float, width: Int, height: Int) {
        val firstTwo = currentDisplayNumber.substring(0, 2)
        val lastTwo = currentDisplayNumber.substring(2, 4)
        
        // Paint for red background
        val redPaint = Paint().apply {
            color = ContextCompat.getColor(this@CameraActivity, R.color.number_red_background)
            style = Paint.Style.FILL
        }
        
        // Paint for blue background
        val bluePaint = Paint().apply {
            color = ContextCompat.getColor(this@CameraActivity, R.color.number_blue_background)
            style = Paint.Style.FILL
        }
        
        // Paint for text
        val textPaint = Paint().apply {
            color = ContextCompat.getColor(this@CameraActivity, R.color.number_text_color)
            textSize = height * 0.671f  // Set to achieve exactly 220px text size
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        
        val halfWidth = width / 2f
        
        // Draw red background for first two digits
        canvas.drawRect(x, y, x + halfWidth, y + height, redPaint)
        
        // Draw blue background for last two digits
        canvas.drawRect(x + halfWidth, y, x + width, y + height, bluePaint)
        
        // Calculate text position
        val textY = y + height / 2f + textPaint.textSize / 3f
        
        // Draw text
        canvas.drawText(firstTwo, x + halfWidth / 2f, textY, textPaint)
        canvas.drawText(lastTwo, x + halfWidth + halfWidth / 2f, textY, textPaint)
    }
    
    private fun notifyMediaScanner(imageUri: android.net.Uri) {
        try {
            // Use MediaScannerConnection for better compatibility
            android.media.MediaScannerConnection.scanFile(
                this,
                arrayOf(imageUri.toString()),
                arrayOf("image/jpeg")
            ) { path, _ ->
                Log.d(TAG, "Media scan completed: $path")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying media scanner", e)
        }
    }
    
    /**
     * Show loading overlay to block user interactions during photo saving
     */
    private fun showLoadingOverlay() {
        binding.loadingOverlay.visibility = android.view.View.VISIBLE
        binding.btnCapture.isClickable = false
        binding.llNumberDisplay.isClickable = false
    }
    
    /**
     * Hide loading overlay and re-enable user interactions
     */
    private fun hideLoadingOverlay() {
        binding.loadingOverlay.visibility = android.view.View.GONE
        binding.btnCapture.isClickable = true
        binding.llNumberDisplay.isClickable = true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}