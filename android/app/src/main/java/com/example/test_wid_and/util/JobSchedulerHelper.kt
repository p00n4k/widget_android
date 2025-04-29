package com.example.test_wid_and.util

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import android.appwidget.AppWidgetManager
import com.example.test_wid_and.service.WidgetUpdateForegroundService
import com.example.test_wid_and.service.WidgetUpdateJobService
import com.example.test_wid_and.widget.MyHomeMediumWidget
import com.example.test_wid_and.widget.MyHomeSmallWidget
import java.util.concurrent.TimeUnit

object JobSchedulerHelper {
    private const val TAG = "JobSchedulerHelper"
    
    // Check if any widgets are currently active
    fun hasActiveWidgets(context: Context): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        
        // Check medium widgets
        val mediumWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MyHomeMediumWidget::class.java)
        )
        
        // Check small widgets
        val smallWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MyHomeSmallWidget::class.java)
        )
        
        val totalWidgets = mediumWidgetIds.size + smallWidgetIds.size
        Log.d(TAG, "Found $totalWidgets active widgets")
        
        return totalWidgets > 0
    }
    
    // Cancel any scheduled widget update jobs
    fun cancelWidgetUpdateJob(context: Context) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancel(WidgetUpdateJobService.JOB_ID)
        Log.d(TAG, "Widget update job canceled")
    }
    
    // Schedule the job to update widgets every 15 minutes
    fun scheduleWidgetUpdateJob(context: Context) {
        // First check if there are any widgets to update
        if (!hasActiveWidgets(context)) {
            Log.d(TAG, "No widgets found, not scheduling updates")
            // Cancel any existing jobs since they're not needed
            cancelWidgetUpdateJob(context)
            return
        }
        
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val componentName = ComponentName(context, WidgetUpdateJobService::class.java)
        
        // Cancel any existing jobs with this ID
        jobScheduler.cancel(WidgetUpdateJobService.JOB_ID)
        
        // Configure update interval - 15 minutes
        val intervalMillis = TimeUnit.MINUTES.toMillis(15)
        
        val jobInfoBuilder = JobInfo.Builder(WidgetUpdateJobService.JOB_ID, componentName)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true) // Job persists across reboots
        
        // Set periodic timing based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // For Android 7+ (Nougat), we can use flex interval
            // Setting flex to 5 minutes gives system some flexibility for battery optimization
            jobInfoBuilder.setPeriodic(
                intervalMillis,
                TimeUnit.MINUTES.toMillis(5)
            )
        } else {
            jobInfoBuilder.setPeriodic(intervalMillis)
        }
        
        // Schedule the job
        val result = jobScheduler.schedule(jobInfoBuilder.build())
        
        if (result == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Widget update job scheduled successfully every 15 minutes")
        } else {
            Log.e(TAG, "Failed to schedule widget update job")
        }
    }
    
    // Run immediate widget update using the foreground service
    fun runImmediateWidgetUpdate(context: Context) {
        // Only update if there are widgets to update
        if (hasActiveWidgets(context)) {
            Log.d(TAG, "Running immediate widget update")
            WidgetUpdateForegroundService.startService(context)
        } else {
            Log.d(TAG, "No widgets to update, skipping immediate update")
        }
    }
}