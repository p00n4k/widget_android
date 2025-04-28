package com.example.test_wid_and.worker

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.test_wid_and.R
import com.example.test_wid_and.service.PM25Service
import com.example.test_wid_and.util.WidgetUtil
import com.example.test_wid_and.widget.MyHomeMediumWidget
import com.example.test_wid_and.widget.MyHomeSmallWidget
import es.antonborri.home_widget.HomeWidgetPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(context: Context, params: WorkerParameters) : 
    CoroutineWorker(context, params) {
        
    companion object {
        private const val TAG = "WidgetUpdateWorker"
        const val WORK_NAME = "widget_update_work"
        
        fun enqueuePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
                
            val periodicRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                15, TimeUnit.MINUTES,  // Minimum interval in WorkManager is 15 minutes
                5, TimeUnit.MINUTES    // Flex period
            )
                .setConstraints(constraints)
                .build()
                
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
            
            // For immediate update (e.g., when app starts or widget is added)
            val oneTimeRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setConstraints(constraints)
                .build()
                
            WorkManager.getInstance(context).enqueue(oneTimeRequest)
            
            Log.d(TAG, "Scheduled periodic widget updates every 15 minutes and immediate update")
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val currentTime = WidgetUtil.getCurrentTimeFormatted()
            Log.d(TAG, "Widget update started at $currentTime")
            
            // Get location data from HomeWidgetPlugin
            val widgetData = HomeWidgetPlugin.getData(applicationContext)
            val locationDataString = widgetData.getString("locationData_from_flutter_APP_new_5", null)
            
            if (locationDataString == null) {
                Log.w(TAG, "No location data found")
                return@withContext Result.failure()
            }
            
            val parts = locationDataString.split(",")
            if (parts.size < 2) {
                Log.e(TAG, "Invalid location data format: $locationDataString")
                return@withContext Result.failure()
            }
            
            val latitude = parts[0].toDoubleOrNull()
            val longitude = parts[1].toDoubleOrNull()
            
            if (latitude == null || longitude == null) {
                Log.e(TAG, "Failed to parse coordinates: $locationDataString")
                return@withContext Result.failure()
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
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widgets", e)
            Result.failure()
        }
    }
}