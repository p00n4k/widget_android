package com.example.test_wid_and

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.example.test_wid_and.util.JobSchedulerHelper

class WidgetApplication : Application(), Configuration.Provider, LifecycleObserver {
    companion object {
        private const val TAG = "WidgetApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created, setting up widget background updates")
        
        // Initialize persistent widget updates with JobScheduler
        JobSchedulerHelper.scheduleWidgetUpdateJob(this)
        
        // Register as lifecycle observer to detect when app comes to foreground
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Log.d(TAG, "App came to foreground - triggering immediate widget update")
        
        // Trigger an immediate widget update when app is foregrounded
        JobSchedulerHelper.runImmediateWidgetUpdate(this)
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
    }
}