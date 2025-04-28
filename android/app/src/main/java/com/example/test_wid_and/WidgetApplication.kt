// android/app/src/main/java/com/example/test_wid_and/WidgetApplication.kt
package com.example.test_wid_and

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.test_wid_and.worker.WidgetUpdateWorker

class WidgetApplication : Application(), Configuration.Provider {
    companion object {
        private const val TAG = "WidgetApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created, setting up widget background updates")
        
        // Initialize periodic widget updates when app starts
        WidgetUpdateWorker.enqueuePeriodicWork(this)
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
    }
}