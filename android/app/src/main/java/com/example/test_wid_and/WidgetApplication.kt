package com.example.test_wid_and

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.test_wid_and.service.WidgetUpdateForegroundService
import com.example.test_wid_and.util.BatteryOptimizationHelper
import com.example.test_wid_and.util.JobSchedulerHelper

class WidgetApplication : Application(), LifecycleObserver {
    companion object {
        private const val TAG = "WidgetApplication"
        
        // Track app foreground state to prevent excessive updates
        private var wasInBackground = true
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created, setting up widget background updates")
        
        // Check battery optimization status
        val isBatteryOptimized = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)
        Log.d(TAG, "Battery optimization ignored: $isBatteryOptimized")
        
        // Initialize widget update mechanisms - use JobScheduler only
        JobSchedulerHelper.scheduleWidgetUpdateJob(this)
        
        // Register as lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // Force immediate first update
        WidgetUpdateForegroundService.startService(this)
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        // Only update if app was previously in background
        if (wasInBackground) {
            Log.d(TAG, "App came to foreground from background - triggering immediate widget update")
            WidgetUpdateForegroundService.startService(this)
            wasInBackground = false
        }
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Log.d(TAG, "App went to background")
        wasInBackground = true
    }
}