package com.fotocammera.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_NAME = "fotocamera_prefs"
        private const val KEY_DISPLAY_NUMBER = "display_number"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_DONT_ASK_DEFAULT = "dont_ask_default"
        private const val DEFAULT_NUMBER = "0000"
    }
    
    /**
     * Save the 4-digit display number
     */
    fun saveDisplayNumber(number: String) {
        sharedPreferences.edit()
            .putString(KEY_DISPLAY_NUMBER, number)
            .apply()
    }
    
    /**
     * Get the saved 4-digit display number
     */
    fun getDisplayNumber(): String {
        return sharedPreferences.getString(KEY_DISPLAY_NUMBER, DEFAULT_NUMBER) ?: DEFAULT_NUMBER
    }
    
    /**
     * Check if this is the first launch of the app
     */
    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    /**
     * Mark that the app has been launched before
     */
    fun setFirstLaunchCompleted() {
        sharedPreferences.edit()
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .apply()
    }
    
    /**
     * Check if user doesn't want to be asked about default camera setting
     */
    fun isDontAskAgainDefault(): Boolean {
        return sharedPreferences.getBoolean(KEY_DONT_ASK_DEFAULT, false)
    }
    
    /**
     * Set the don't ask again preference for default camera dialog
     */
    fun setDontAskAgainDefault(dontAsk: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_DONT_ASK_DEFAULT, dontAsk)
            .apply()
    }
    
    /**
     * Reset all preferences (for testing purposes)
     */
    fun resetPreferences() {
        sharedPreferences.edit()
            .clear()
            .apply()
    }
}