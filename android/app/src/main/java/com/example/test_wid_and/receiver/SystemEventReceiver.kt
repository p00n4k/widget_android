package com.example.test_wid_and.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.test_wid_and.util.JobSchedulerHelper

/**
 * BroadcastReceiver to handle system events that should trigger widget updates.
 */
class SystemEventReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SystemEventReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received system event: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (JobSchedulerHelper.hasActiveWidgets(context)) {
                    Log.d(TAG, "Device booted, widgets exist, restarting updates")
                    JobSchedulerHelper.scheduleWidgetUpdateJob(context)
                }
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (JobSchedulerHelper.hasActiveWidgets(context)) {
                    Log.d(TAG, "App updated, widgets exist, restarting updates")
                    JobSchedulerHelper.scheduleWidgetUpdateJob(context)
                }
            }
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                if (JobSchedulerHelper.hasActiveWidgets(context)) {
                    Log.d(TAG, "Time/timezone changed, widgets exist, updating")
                    JobSchedulerHelper.runImmediateWidgetUpdate(context)
                }
            }
        }
    }
}