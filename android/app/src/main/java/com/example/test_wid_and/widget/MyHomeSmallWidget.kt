package com.example.test_wid_and.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.test_wid_and.util.JobSchedulerHelper

/**
 * Implementation of App Widget functionality for small widget.
 */
class MyHomeSmallWidget : AppWidgetProvider() {
    companion object {
        private const val TAG = "MyHomeSmallWidget"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called with ${appWidgetIds.size} widgets")
        
        // Schedule the JobScheduler for persistent updates
        JobSchedulerHelper.scheduleWidgetUpdateJob(context)
        
        // Run an immediate update
        JobSchedulerHelper.runImmediateWidgetUpdate(context)
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "Widget enabled, scheduling updates")
        JobSchedulerHelper.scheduleWidgetUpdateJob(context)
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        // Handle manual refresh if needed
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            Log.d(TAG, "Received widget update request")
            JobSchedulerHelper.runImmediateWidgetUpdate(context)
        }
    }
}