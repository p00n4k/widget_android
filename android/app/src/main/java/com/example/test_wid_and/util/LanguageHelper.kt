package com.example.test_wid_and.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import java.util.Locale

object LanguageHelper {
    private const val TAG = "LanguageHelper"
    
    fun getConfiguredContext(context: Context): Context {
        val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("language_code", "en") ?: "en"
        
        Log.d(TAG, "Getting configured context with language: $languageCode")
        
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
    
    fun getLocalizedResources(context: Context): Resources {
        val configContext = getConfiguredContext(context)
        return configContext.resources
    }
    
    fun getStringResource(context: Context, resId: Int): String {
        val resources = getLocalizedResources(context)
        return resources.getString(resId)
    }
    
    fun getCurrentLanguageCode(context: Context): String {
        val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return prefs.getString("language_code", "en") ?: "en"
    }
}