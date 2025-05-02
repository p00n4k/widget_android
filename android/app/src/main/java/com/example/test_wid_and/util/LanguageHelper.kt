package com.example.test_wid_and.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import java.util.Locale
import android.os.Build

object LanguageHelper {
    private const val TAG = "LanguageHelper"
    
    // Cache for configured resources to avoid recreating them frequently
    private var cachedResources: Resources? = null
    private var cachedLanguageCode: String? = null
    
    fun getConfiguredContext(context: Context): Context {
        val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("language_code", "en") ?: "en"
        
        // If already configured for this language, return immediately
        if (cachedResources != null && cachedLanguageCode == languageCode) {
            return context
        }
        
        Log.d(TAG, "Configuring context with language: $languageCode")
        
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        // Update cache
        cachedLanguageCode = languageCode
        
        return context.createConfigurationContext(config)
    }
    
    fun getLocalizedResources(context: Context): Resources {
        val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("language_code", "en") ?: "en"
        
        // Use cached resources if available
        if (cachedResources != null && cachedLanguageCode == languageCode) {
            return cachedResources!!
        }
        
        val configContext = getConfiguredContext(context)
        cachedResources = configContext.resources
        return cachedResources!!
    }
    
    fun getStringResource(context: Context, resId: Int): String {
        return getLocalizedResources(context).getString(resId)
    }
    
    fun getCurrentLanguageCode(context: Context): String {
        val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return prefs.getString("language_code", "en") ?: "en"
    }
}