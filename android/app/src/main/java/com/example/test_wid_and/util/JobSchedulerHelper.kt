package com.example.test_wid_and.util

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import android.util.Log
import com.example.test_wid_and.service.WidgetUpdateJobService
import java.util.concurrent.TimeUnit

object JobSchedulerHelper {
    private const val TAG = "JobSchedulerHelper"
    
    // Schedule the job to update widgets every 15 minutes
    fun scheduleWidgetUpdateJob(context: Context) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val componentName = ComponentName(context, WidgetUpdateJobService::class.java)
        
        val jobInfoBuilder = JobInfo.Builder(WidgetUpdateJobService.JOB_ID, componentName)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true) // Job persists across reboots
            .setPeriodic(TimeUnit.MINUTES.toMillis(15)) // 15 minute interval
            
        // Schedule the job
        val result = jobScheduler.schedule(jobInfoBuilder.build())
        
        if (result == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Widget update job scheduled successfully")
        } else {
            Log.e(TAG, "Failed to schedule widget update job")
        }
    }
    
    // Run immediate widget update
    fun runImmediateWidgetUpdate(context: Context) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val componentName = ComponentName(context, WidgetUpdateJobService::class.java)
        
        // Create a one-time job for immediate execution
        val jobInfoBuilder = JobInfo.Builder(WidgetUpdateJobService.JOB_ID + 1, componentName)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setOverrideDeadline(0) // Run immediately
            
        // Schedule the immediate job
        val result = jobScheduler.schedule(jobInfoBuilder.build())
        
        if (result == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Immediate widget update job scheduled")
        } else {
            Log.e(TAG, "Failed to schedule immediate widget update job")
        }
    }
}