package com.fotocammera

import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.WindowManager
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
        
        // Get actual device rotation from window manager for better orientation handling
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val actualRotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }
        
        // Use device orientation to determine proper rotation
        val deviceOrientation = resources.configuration.orientation
        val targetRotation = when {
            deviceOrientation == Configuration.ORIENTATION_PORTRAIT && actualRotation == Surface.ROTATION_0 -> Surface.ROTATION_0
            deviceOrientation == Configuration.ORIENTATION_PORTRAIT && actualRotation == Surface.ROTATION_180 -> Surface.ROTATION_180  
            deviceOrientation == Configuration.ORIENTATION_LANDSCAPE && actualRotation == Surface.ROTATION_90 -> Surface.ROTATION_90
            deviceOrientation == Configuration.ORIENTATION_LANDSCAPE && actualRotation == Surface.ROTATION_270 -> Surface.ROTATION_270
            else -> actualRotation
        }
        
        imageCapture.targetRotation = targetRotation
        
        // Capture current orientation for overlay positioning  
        val currentOrientation = resources.configuration.orientation
        
        Log.d(TAG, "Device orientation: ${if (deviceOrientation == Configuration.ORIENTATION_PORTRAIT) "PORTRAIT" else "LANDSCAPE"}")
        Log.d(TAG, "Actual rotation: $actualRotation")
        Log.d(TAG, "Target rotation set to: $targetRotation")
        Log.d(TAG, "Configuration orientation: $currentOrientation")
        
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
                    showSuccessOverlay(generateFileName())
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
            // Read EXIF data to check rotation
            val inputStreamExif = contentResolver.openInputStream(imageUri)
            val exif = androidx.exifinterface.media.ExifInterface(inputStreamExif!!)
            inputStreamExif.close()
            
            val exifOrientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
            
            // Read the original image
            val inputStream = contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap != null) {
                Log.d(TAG, "Original image dimensions: ${originalBitmap.width} x ${originalBitmap.height}")
                Log.d(TAG, "Device orientation: ${if (deviceOrientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"}")
                Log.d(TAG, "EXIF orientation: $exifOrientation")
                
                // Actually rotate the bitmap based on EXIF orientation
                val rotatedBitmap = when (exifOrientation) {
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> {
                        Log.d(TAG, "Rotating image 90 degrees")
                        rotateBitmap(originalBitmap, 90f)
                    }
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> {
                        Log.d(TAG, "Rotating image 180 degrees")
                        rotateBitmap(originalBitmap, 180f)
                    }
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> {
                        Log.d(TAG, "Rotating image 270 degrees")
                        rotateBitmap(originalBitmap, 270f)
                    }
                    else -> {
                        Log.d(TAG, "No rotation needed")
                        originalBitmap
                    }
                }
                
                val finalWidth = rotatedBitmap.width
                val finalHeight = rotatedBitmap.height
                
                Log.d(TAG, "Final image dimensions after rotation: ${finalWidth} x ${finalHeight}")
                
                // Create a mutable copy of the rotated bitmap
                val mutableBitmap = rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutableBitmap)
                
                // Calculate overlay size based on final rotated dimensions
                val scaleFactor = minOf(finalWidth, finalHeight) / 1000f
                val baseWidth = (616 * 0.6 * 0.65).toInt()  // 40% + 35% reduction: 616 * 0.6 * 0.65 = 240
                val baseHeight = (328 * 0.6 * 0.65).toInt() // 40% + 35% reduction: 328 * 0.6 * 0.65 = 128
                val numberWidth = (baseWidth * scaleFactor).toInt().coerceAtLeast(120)  // Further reduced minimum
                val numberHeight = (baseHeight * scaleFactor).toInt().coerceAtLeast(60) // Further reduced minimum
                
                val margin = 20 // Small margin from edges
                
                // Position based on final rotated image dimensions
                val x = finalWidth - numberWidth - margin
                val y = finalHeight - numberHeight - margin
                
                Log.d(TAG, "Overlay position: ($x, $y), size: ${numberWidth}x${numberHeight}")
                Log.d(TAG, "Using rotated image dimensions: ${finalWidth}x${finalHeight}")
                
                // Draw number background and text
                drawNumberOverlay(canvas, x.toFloat(), y.toFloat(), numberWidth, numberHeight)
                
                // Save the modified image back (now physically rotated correctly)
                val outputStream = contentResolver.openOutputStream(imageUri)
                outputStream?.use {
                    mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                }
                
                Log.d(TAG, "Image saved with physical rotation applied")
                
                // Clean up bitmaps
                if (rotatedBitmap != originalBitmap) {
                    originalBitmap.recycle()
                }
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
    
    /**
     * Show success overlay with filename and auto-hide after delay
     */
    private fun showSuccessOverlay(filename: String) {
        binding.tvSuccessFilename.text = filename
        binding.successOverlay.visibility = android.view.View.VISIBLE
        
        // Auto-hide after 2 seconds
        binding.successOverlay.postDelayed({
            hideSuccessOverlay()
        }, 2000)
    }
    
    /**
     * Hide success overlay
     */
    private fun hideSuccessOverlay() {
        binding.successOverlay.visibility = android.view.View.GONE
    }
    
    /**
     * Rotate a bitmap by the specified degrees
     */
    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}