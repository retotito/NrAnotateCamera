package com.fotocammera

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.widget.NumberPicker
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.fotocammera.databinding.DialogNumberPickerBinding
import java.lang.reflect.Field

class NumberPickerDialog(
    private val context: Context,
    private val currentNumber: String,
    private val onNumberSelected: (String) -> Unit
) {
    
    private var dialog: AlertDialog? = null
    
    fun show() {
        val binding = DialogNumberPickerBinding.inflate(LayoutInflater.from(context))
        
        // Setup number pickers with styling
        setupNumberPicker(binding.npFirstDigit, currentNumber.getOrNull(0)?.toString()?.toIntOrNull() ?: 0, true)
        setupNumberPicker(binding.npSecondDigit, currentNumber.getOrNull(1)?.toString()?.toIntOrNull() ?: 0, true)
        setupNumberPicker(binding.npThirdDigit, currentNumber.getOrNull(2)?.toString()?.toIntOrNull() ?: 0, false)
        setupNumberPicker(binding.npFourthDigit, currentNumber.getOrNull(3)?.toString()?.toIntOrNull() ?: 0, false)
        
        dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create()
        
        // Handle button clicks
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnOk.setOnClickListener {
            val selectedNumber = String.format(
                "%d%d%d%d",
                binding.npFirstDigit.value,
                binding.npSecondDigit.value,
                binding.npThirdDigit.value,
                binding.npFourthDigit.value
            )
            onNumberSelected(selectedNumber)
            dismiss()
        }
        
        dialog?.show()
    }
    
    private fun setupNumberPicker(picker: NumberPicker, initialValue: Int, isRed: Boolean) {
        picker.apply {
            minValue = 0
            maxValue = 9
            value = initialValue
            wrapSelectorWheel = true
            
            // Style the NumberPicker
            styleNumberPicker(this, isRed)
        }
    }
    
    private fun styleNumberPicker(picker: NumberPicker, isRed: Boolean) {
        try {
            // Increase text size and apply styling
            val wheelPaintField: Field = picker.javaClass.getDeclaredField("mSelectorWheelPaint")
            wheelPaintField.isAccessible = true
            val wheelPaint = wheelPaintField.get(picker) as android.graphics.Paint
            wheelPaint.textSize = 72f // Increased from default ~48f
            
            // Set divider height to 0 to remove lines
            val dividerField: Field = picker.javaClass.getDeclaredField("mSelectionDividerHeight")
            dividerField.isAccessible = true
            dividerField.setInt(picker, 0)
            
            // Style all child TextViews
            for (i in 0 until picker.childCount) {
                val child = picker.getChildAt(i)
                if (child is TextView) {
                    child.textSize = 24f // Larger text size
                    child.typeface = Typeface.DEFAULT_BOLD // Bold text
                    child.setTextColor(Color.WHITE) // White text
                    
                    // Apply red or blue background to selected (middle) TextView
                    if (i == 1) { // Middle child is typically the selected one
                        val backgroundColor = if (isRed) {
                            ContextCompat.getColor(context, R.color.number_red_background)
                        } else {
                            ContextCompat.getColor(context, R.color.number_blue_background)
                        }
                        child.setBackgroundColor(backgroundColor)
                        child.setPadding(16, 8, 16, 8)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback styling if reflection fails
            e.printStackTrace()
        }
    }
    
    private fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}