package com.example.test_wid_and.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log

class WidgetUpdateJobService : JobService() {
    companion object {
        private const val TAG = "WidgetUpdateJobService"
        const val JOB_ID = 1000
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Periodic widget update job started")
        
        // Start the foreground service to ensure update happens reliably
        WidgetUpdateForegroundService.startService(this)
        
        // Tell system our job is done - the foreground service handles the actual update
        jobFinished(params, false)
        
        // Return false because we're not doing work in background with this JobService
        // The actual work is delegated to the foreground service
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Widget update job stopped")
        // Return true to reschedule the job if it's stopped unexpectedly
        return true
    }
}