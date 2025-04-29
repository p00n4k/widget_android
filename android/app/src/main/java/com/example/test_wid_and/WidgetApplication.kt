package com.example.test_wid_and

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.example.test_wid_and.service.WidgetUpdateForegroundService
import com.example.test_wid_and.util.BatteryOptimizationHelper
import com.example.test_wid_and.util.JobSchedulerHelper

class WidgetApplication : Application(), Configuration.Provider, LifecycleObserver {
    companion object {
        private const val TAG = "WidgetApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created, setting up widget background updates")
        
        // Check battery optimization status
        val isBatteryOptimized = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)
        Log.d(TAG, "Battery optimization ignored: $isBatteryOptimized")
        
        // Initialize widget update mechanisms
        JobSchedulerHelper.scheduleWidgetUpdateJob(this)
        
        // Register as lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // Force immediate first update
        WidgetUpdateForegroundService.startService(this)
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Log.d(TAG, "App came to foreground - triggering immediate widget update")
        WidgetUpdateForegroundService.startService(this)
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
    }
}