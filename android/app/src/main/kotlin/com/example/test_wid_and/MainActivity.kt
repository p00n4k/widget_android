package com.example.test_wid_and

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
import java.util.Locale
import android.content.res.Configuration
import android.content.SharedPreferences
import android.util.Log
import android.content.ComponentName
import com.example.test_wid_and.service.WidgetUpdateForegroundService
import com.example.test_wid_and.widget.MyHomeMediumWidget
import com.example.test_wid_and.widget.MyHomeSmallWidget
import android.appwidget.AppWidgetManager

class MainActivity: FlutterActivity() {
    private val BATTERY_CHANNEL = "com.example.test_wid_and/battery_optimization"
    private val LANGUAGE_CHANNEL = "com.check_phoon_widget/language"
    private val TAG = "MainActivity"
    private var isInitializing = true // Flag to prevent recursive recreate calls
    private var currentLanguage: String? = null // Track current language to avoid unnecessary updates
    
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine)
        
        // Initialize language from saved preference WITHOUT recreating the activity
        if (isInitializing) {
            initializeLanguage()
            isInitializing = false
        }
        
        // Battery optimization channel
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
        
        // Language channel
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, LANGUAGE_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "changeLanguage" -> {
                    val language = call.argument<String>("language") ?: "Eng"
                    val locale = mapFlutterLanguageToLocale(language)
                    
                    // Check if language is actually different to avoid unnecessary updates
                    if (locale.language != currentLanguage) {
                        updateLocale(locale, recreateActivity = true)
                        result.success(true)
                    } else {
                        // Language hasn't changed, no need to update
                        result.success(false)
                    }
                }
                "getCurrentLanguage" -> {
                    val prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
                    val currentLang = prefs.getString("language_code", "en")
                    result.success(if (currentLang == "en") "Eng" else "ไทย")
                }
                else -> result.notImplemented()
            }
        }
    }
    
    private fun mapFlutterLanguageToLocale(flutterLanguage: String): Locale {
        return when (flutterLanguage) {
            "ไทย" -> Locale("th")
            else -> Locale("en") // Default to English
        }
    }
    
    private fun updateLocale(locale: Locale, recreateActivity: Boolean = false) {
        // Skip update if language hasn't changed
        if (locale.language == currentLanguage) {
            return
        }
        
        Locale.setDefault(locale)
        currentLanguage = locale.language
        
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        
        resources.updateConfiguration(config, resources.displayMetrics)
        
        // Save preference
        val prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("language_code", locale.language)
        editor.apply()
        
        // Log language change
        Log.d(TAG, "Language changed to: ${locale.language}")
        
        // Use a single Intent to update all widgets
        val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        sendBroadcast(updateIntent)
        
        // Schedule one widget update job instead of starting service immediately
        WidgetUpdateForegroundService.startService(this)
        
        // Only recreate if explicitly requested (not during initialization)
        if (recreateActivity) {
            recreate()
        }
    }
    
    private fun initializeLanguage() {
        val prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val savedLanguage = prefs.getString("language_code", "en") ?: "en"
        currentLanguage = savedLanguage
        
        val locale = Locale(savedLanguage)
        
        // Don't recreate during initialization
        updateLocale(locale, recreateActivity = false)
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
}