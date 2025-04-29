package com.example.test_wid_and.util

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.test_wid_and.service.WidgetUpdateForegroundService
import com.example.test_wid_and.service.WidgetUpdateJobService
import java.util.concurrent.TimeUnit

object JobSchedulerHelper {
    private const val TAG = "JobSchedulerHelper"
    
    // Schedule the job to update widgets every 15 minutes
    fun scheduleWidgetUpdateJob(context: Context) {
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
        Log.d(TAG, "Running immediate widget update")
        WidgetUpdateForegroundService.startService(context)
    }
}