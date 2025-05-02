package com.example.test_wid_and.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.test_wid_and.MainActivity
import com.example.test_wid_and.R
import com.example.test_wid_and.util.WidgetUtil
import com.example.test_wid_and.util.JobSchedulerHelper
import com.example.test_wid_and.util.LanguageHelper
import com.example.test_wid_and.widget.MyHomeMediumWidget
import com.example.test_wid_and.widget.MyHomeSmallWidget
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import es.antonborri.home_widget.HomeWidgetPlugin
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class WidgetUpdateForegroundService : Service() {
    companion object {
        private const val TAG = "WidgetUpdateFgService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "widget_update_channel"
        
        // Flag to prevent multiple simultaneous updates
        private val isUpdating = AtomicBoolean(false)
        
        // Last update timestamp to throttle updates
        private var lastUpdateTime = 0L
        private const val MIN_UPDATE_INTERVAL = 2000 // 2 seconds
        
        fun startService(context: Context) {
            // Only start if not already updating and not too soon after last update
            val now = System.currentTimeMillis()
            if (!isUpdating.get() && now - lastUpdateTime > MIN_UPDATE_INTERVAL) {
                if (isUpdating.compareAndSet(false, true)) {
                    lastUpdateTime = now
                    val intent = Intent(context, WidgetUpdateForegroundService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                    Log.d(TAG, "Service start requested")
                }
            } else {
                Log.d(TAG, "Update already in progress or too soon after last update, skipping")
            }
        }
    }
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    // Cache notification objects
    private var cachedNotification: Notification? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started, updating widgets")
        
        // Show a foreground notification
        val notification = cachedNotification ?: createNotification().also { cachedNotification = it }
        startForeground(NOTIFICATION_ID, notification)
        
        // Update widgets in background
        serviceScope.launch {
            try {
                updateWidgets()
                // Stop the service after update
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widgets", e)
                stopSelf()
            } finally {
                // Reset the updating flag when done
                isUpdating.set(false)
            }
        }
        
        // If service is killed, don't restart automatically (JobScheduler will handle scheduling)
        return START_NOT_STICKY
    }
    
    private suspend fun updateWidgets() {
        val currentTime = WidgetUtil.getCurrentTimeFormatted()
        Log.d(TAG, "Widget update started at $currentTime in foreground service")

        // Check if any widgets exist before proceeding
        if (!JobSchedulerHelper.hasActiveWidgets(applicationContext)) {
            Log.d(TAG, "No widgets found, canceling update")
            return
        }
        
        // Get data from HomeWidgetPlugin
        val widgetData = HomeWidgetPlugin.getData(applicationContext)
        val locationDataString = widgetData.getString("locationData_from_flutter_APP_new_5", null)
        
        if (locationDataString == null) {
            Log.w(TAG, "No location data found")
            return
        }
        
        val parts = locationDataString.split(",")
        if (parts.size < 2) {
            Log.e(TAG, "Invalid location data format: $locationDataString")
            return
        }
        
        val latitude = parts[0].toDoubleOrNull()
        val longitude = parts[1].toDoubleOrNull()
        
        // Get language code from parts or use device preference
        val languageCode = if (parts.size >= 3) {
            when (parts[2]) {
                "Eng" -> "en"
                "ไทย" -> "th"
                else -> "en" // Default
            }
        } else {
            // Get system preference
            LanguageHelper.getCurrentLanguageCode(applicationContext)
        }
        
        Log.d(TAG, "Language code for widget update: $languageCode")
        
        if (latitude == null || longitude == null) {
            Log.e(TAG, "Failed to parse coordinates: $locationDataString")
            return
        }
        
        Log.d(TAG, "Fetching PM2.5 data for location: $latitude, $longitude, language: $languageCode")
        
        // Fetch PM2.5 data with language code
        val pm25Data = PM25Service.fetchPM25Data(latitude, longitude, languageCode)
        
        Log.d(TAG, "PM2.5 data received: ${pm25Data.pmCurrent ?: getString(R.string.no_data)} μg/m³")
        
        // Update both widget types efficiently
        updateWidgetsByType(R.layout.my_home_medium_widget, MyHomeMediumWidget::class.java, pm25Data)
        updateWidgetsByType(R.layout.my_home_small_widget, MyHomeSmallWidget::class.java, pm25Data)
        
        val endTime = WidgetUtil.getCurrentTimeFormatted()
        Log.d(TAG, "Widget update completed successfully at $endTime")
    }
    
    private fun updateWidgetsByType(layoutId: Int, widgetClass: Class<*>, pm25Data: WidgetUtil.PM25Data) {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(applicationContext, widgetClass)
        )
        
        Log.d(TAG, "Updating ${widgetIds.size} widgets of type ${widgetClass.simpleName}")
        
        if (widgetIds.isNotEmpty()) {
            // Build views once and reuse for all widgets of same type
            val views = WidgetUtil.buildWidgetViews(
                applicationContext, 
                layoutId,
                pm25Data
            )
            
            for (widgetId in widgetIds) {
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Get localized strings for channel
            val title = LanguageHelper.getStringResource(this, R.string.updating_widget_title)
            val description = LanguageHelper.getStringResource(this, R.string.updating_widget_text)
            
            val channel = NotificationChannel(
                CHANNEL_ID,
                title,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                this.description = description
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(LanguageHelper.getStringResource(this, R.string.updating_widget_title))
            .setContentText(LanguageHelper.getStringResource(this, R.string.updating_widget_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "Service destroyed")
    }
}