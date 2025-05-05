package com.check_phoon.android_widget.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object BatteryOptimizationHelper {
    private const val TAG = "BatteryOptimHelper"
    
    // Check if app is ignoring battery optimizations
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // On older versions, battery optimizations were less aggressive
        }
    }
    
    // Create intent to request battery optimization exemption
    fun getBatteryOptimizationIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(context)) {
                Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:${context.packageName}")
                }
            } else {
                Log.d(TAG, "App is already ignoring battery optimizations")
                null
            }
        } else {
            Log.d(TAG, "Battery optimization settings not needed for Android < M")
            null
        }
    }
}