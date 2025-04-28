package com.example.test_wid_and.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.util.Log
import com.example.test_wid_and.R
import com.example.test_wid_and.util.WidgetUtil
import com.example.test_wid_and.widget.MyHomeMediumWidget
import com.example.test_wid_and.widget.MyHomeSmallWidget
import es.antonborri.home_widget.HomeWidgetPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WidgetUpdateJobService : JobService() {
    companion object {
        private const val TAG = "WidgetUpdateJobService"
        const val JOB_ID = 1000
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Widget update job started")
        
        // Perform the widget update in a coroutine
        scope.launch {
            try {
                val currentTime = WidgetUtil.getCurrentTimeFormatted()
                Log.d(TAG, "Widget update started at $currentTime")
                
                // Get location data from HomeWidgetPlugin
                val widgetData = HomeWidgetPlugin.getData(applicationContext)
                val locationDataString = widgetData.getString("locationData_from_flutter_APP_new_5", null)
                
                if (locationDataString == null) {
                    Log.w(TAG, "No location data found")
                    jobFinished(params, true) // Reschedule on failure
                    return@launch
                }
                
                val parts = locationDataString.split(",")
                if (parts.size < 2) {
                    Log.e(TAG, "Invalid location data format: $locationDataString")
                    jobFinished(params, true)
                    return@launch
                }
                
                val latitude = parts[0].toDoubleOrNull()
                val longitude = parts[1].toDoubleOrNull()
                
                if (latitude == null || longitude == null) {
                    Log.e(TAG, "Failed to parse coordinates: $locationDataString")
                    jobFinished(params, true)
                    return@launch
                }
                
                Log.d(TAG, "Fetching PM2.5 data for location: $latitude, $longitude")
                
                // Fetch PM2.5 data
                val pm25Data = PM25Service.fetchPM25Data(latitude, longitude)
                
                Log.d(TAG, "PM2.5 data received: ${pm25Data.pmCurrent ?: "No data"} μg/m³")
                
                // Update medium widget
                val mediumAppWidgetManager = AppWidgetManager.getInstance(applicationContext)
                val mediumWidgetIds = mediumAppWidgetManager.getAppWidgetIds(
                    ComponentName(applicationContext, MyHomeMediumWidget::class.java)
                )
                
                Log.d(TAG, "Updating ${mediumWidgetIds.size} medium widgets")
                
                for (widgetId in mediumWidgetIds) {
                    val views = WidgetUtil.buildWidgetViews(
                        applicationContext, 
                        R.layout.my_home_medium_widget,
                        pm25Data
                    )
                    mediumAppWidgetManager.updateAppWidget(widgetId, views)
                }
                
                // Update small widget
                val smallAppWidgetManager = AppWidgetManager.getInstance(applicationContext)
                val smallWidgetIds = smallAppWidgetManager.getAppWidgetIds(
                    ComponentName(applicationContext, MyHomeSmallWidget::class.java)
                )
                
                Log.d(TAG, "Updating ${smallWidgetIds.size} small widgets")
                
                for (widgetId in smallWidgetIds) {
                    val views = WidgetUtil.buildWidgetViews(
                        applicationContext, 
                        R.layout.my_home_small_widget,
                        pm25Data
                    )
                    smallAppWidgetManager.updateAppWidget(widgetId, views)
                }
                
                val endTime = WidgetUtil.getCurrentTimeFormatted()
                Log.d(TAG, "Widget update completed successfully at $endTime")
                
                // Job complete, don't reschedule
                jobFinished(params, false)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widgets", e)
                // Error occurred, reschedule the job
                jobFinished(params, true)
            }
        }
        
        // Return true as we're doing work on a different thread
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Widget update job stopped")
        job.cancel()
        // Return true to reschedule the job if it's stopped
        return true
    }
}