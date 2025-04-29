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
        Log.d(TAG, "Application created")
        
        // Check battery optimization status
        val isBatteryOptimized = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)
        Log.d(TAG, "Battery optimization ignored: $isBatteryOptimized")
        
        // Only schedule updates if widgets exist
        if (JobSchedulerHelper.hasActiveWidgets(this)) {
            Log.d(TAG, "Widgets found, setting up background updates")
            JobSchedulerHelper.scheduleWidgetUpdateJob(this)
            
            // Force immediate first update
            WidgetUpdateForegroundService.startService(this) 
        } else {
            Log.d(TAG, "No widgets found, skipping update scheduling")
        }
        
        // Register as lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        // Only update if app was previously in background AND widgets exist
        if (wasInBackground && JobSchedulerHelper.hasActiveWidgets(this)) {
            Log.d(TAG, "App came to foreground, widgets exist - triggering update")
            WidgetUpdateForegroundService.startService(this)
            wasInBackground = false
        } else {
            wasInBackground = false
        }
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Log.d(TAG, "App went to background")
        wasInBackground = true
    }
}