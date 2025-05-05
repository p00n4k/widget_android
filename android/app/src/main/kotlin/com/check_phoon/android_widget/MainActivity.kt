package com.check_phoon.android_widget

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.content.Context
import io.flutter.plugins.GeneratedPluginRegistrant
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import java.util.Locale
import android.content.SharedPreferences
import com.check_phoon.android_widget.service.WidgetUpdateForegroundService
import android.appwidget.AppWidgetManager

class MainActivity: FlutterActivity() {
    private val BATTERY_CHANNEL = "com.check_phoon.android_widget/battery_optimization"
    private val LANGUAGE_CHANNEL = "com.check_phoon.android_widget/language"
    private val PREFS_NAME = "WidgetLanguagePrefs"
    private val LANGUAGE_KEY = "language_code"
    private val TAG = "MainActivity"
    
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine)
        
        // Battery optimization method channel
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, BATTERY_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "isIgnoringBatteryOptimizations" -> {
                    result.success(isIgnoringBatteryOptimizations())
                }
                "requestIgnoreBatteryOptimizations" -> {
                    requestIgnoreBatteryOptimizations()
                    result.success(true)
                }
                else -> result.notImplemented()
            }
        }
        
        // Language method channel
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, LANGUAGE_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "changeLanguage" -> {
                    val language = call.argument<String>("language") ?: "Eng"
                    val locale = mapFlutterLanguageToLocale(language)
                    updateLocale(locale)
                    
                    // Force widget updates to refresh with new language
                    refreshWidgets()
                    
                    result.success(true)
                }
                "getCurrentLanguage" -> {
                    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val languageCode = prefs.getString(LANGUAGE_KEY, "en") ?: "en"
                    val flutterLanguage = if (languageCode == "en") "Eng" else "ไทย"
                    result.success(flutterLanguage)
                }
                else -> result.notImplemented()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        initializeLanguage()
    }
    
    private fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
    }
    
    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations()) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
    
    // Map Flutter language to Android Locale
    private fun mapFlutterLanguageToLocale(flutterLanguage: String): Locale {
        return when (flutterLanguage) {
            "ไทย" -> Locale("th")
            else -> Locale("en") // Default to English
        }
    }
    
    // Update app locale and configuration
    private fun updateLocale(locale: Locale) {
        try {
            // Save language preference
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(LANGUAGE_KEY, locale.language)
            editor.apply()
            
            // Set default locale
            Locale.setDefault(locale)
            
            // Update configuration
            val resources = resources
            val config = Configuration(resources.configuration)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLocale(locale)
                createConfigurationContext(config)
                resources.updateConfiguration(config, resources.displayMetrics)
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
                @Suppress("DEPRECATION")
                resources.updateConfiguration(config, resources.displayMetrics)
            }
            
            Log.d(TAG, "Locale updated to: ${locale.language}")
            
            // Note: we don't recreate activity as this breaks Flutter
        } catch (e: Exception) {
            Log.e(TAG, "Error updating locale", e)
        }
    }
    
    // Initialize language based on saved preference
    private fun initializeLanguage() {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val languageCode = prefs.getString(LANGUAGE_KEY, "en") ?: "en"
            val locale = Locale(languageCode)
            updateLocale(locale)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing language", e)
        }
    }
    
    // Force refresh of all widgets
    private fun refreshWidgets() {
        try {
            // Start widget update service for immediate refresh
            WidgetUpdateForegroundService.startService(this)
            
            // Also send a broadcast to update the widgets with new configuration
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            intent.putExtra("language_update", true)
            sendBroadcast(intent)
            
            Log.d(TAG, "Widget refresh requested after language change")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing widgets", e)
        }
    }
}