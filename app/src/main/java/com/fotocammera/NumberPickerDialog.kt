package com.fotocammera

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.NumberPicker
import com.fotocammera.databinding.DialogNumberPickerBinding

class NumberPickerDialog(
    private val context: Context,
    private val currentNumber: String,
    private val onNumberSelected: (String) -> Unit
) {
    
    private var dialog: AlertDialog? = null
    
    fun show() {
        val binding = DialogNumberPickerBinding.inflate(LayoutInflater.from(context))
        
        // Setup number pickers
        setupNumberPicker(binding.npFirstDigit, currentNumber.getOrNull(0)?.toString()?.toIntOrNull() ?: 0)
        setupNumberPicker(binding.npSecondDigit, currentNumber.getOrNull(1)?.toString()?.toIntOrNull() ?: 0)
        setupNumberPicker(binding.npThirdDigit, currentNumber.getOrNull(2)?.toString()?.toIntOrNull() ?: 0)
        setupNumberPicker(binding.npFourthDigit, currentNumber.getOrNull(3)?.toString()?.toIntOrNull() ?: 0)
        
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
    
    private fun setupNumberPicker(picker: NumberPicker, initialValue: Int) {
        picker.apply {
            minValue = 0
            maxValue = 9
            value = initialValue
            wrapSelectorWheel = true
        }
    }
    
    private fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}