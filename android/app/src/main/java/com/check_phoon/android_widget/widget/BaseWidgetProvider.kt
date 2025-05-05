package com.check_phoon.android_widget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.check_phoon.android_widget.service.WidgetUpdateForegroundService
import com.check_phoon.android_widget.util.JobSchedulerHelper

/**
 * Base widget provider with common functionality for all widgets
 */
abstract class BaseWidgetProvider : AppWidgetProvider() {
    abstract val TAG: String

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called with ${appWidgetIds.size} widgets")
        
        // Schedule the JobScheduler for periodic updates
        JobSchedulerHelper.scheduleWidgetUpdateJob(context)
        
        // Run an immediate update using foreground service
        WidgetUpdateForegroundService.startService(context)
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "Widget enabled, scheduling updates")
        JobSchedulerHelper.scheduleWidgetUpdateJob(context)
        
        // Delay immediate update by 1 second to ensure widget is fully initialized
        Handler(Looper.getMainLooper()).postDelayed({
            WidgetUpdateForegroundService.startService(context)
        }, 1000)
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "All widgets of this type removed")
        
        // Check if there are any widgets of any type left
        if (!JobSchedulerHelper.hasActiveWidgets(context)) {
            Log.d(TAG, "No widgets of any type left, canceling all updates")
            JobSchedulerHelper.cancelWidgetUpdateJob(context)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            Log.d(TAG, "Received widget update request")
            WidgetUpdateForegroundService.startService(context)
        }
    }
}