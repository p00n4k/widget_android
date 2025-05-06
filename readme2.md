



I have a flutter app for mobile application. I am working with the android widget. I want to add a new feature; language switching. The language I want is English (default) and Thai. The current code still used hard coded string for each part, so we will have to do some refactor and add a new feature. But I do have `android\app\src\main\res\values\strings.xml` and `android\app\src\main\res\values-th\strings.xml`

For now, I will just show you the english string, so you know the name of each one. Thai string also have the same name but strig will be in thai

`android\app\src\main\res\values\strings.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="appwidget_text">EXAMPLE</string>
    <string name="add_widget">Add widget</string>
    <string name="app_widget_description">This is an app widget description</string>
    
    <!-- Widget Strings -->
    <string name="updating_widget_title">Updating Air Quality Widget</string>
    <string name="updating_widget_text">Fetching latest air quality data...</string>
    <string name="pm25_hourly">PM2.5 Hourly</string>
    <string name="pm25_unit">µg/m³</string>
    <string name="no_data">No data</string>
    
    <!-- Air Quality Levels -->
    <string name="air_very_good">Very Good Air</string>
    <string name="air_good">Good Air</string>
    <string name="air_moderate">Moderate</string>
    <string name="air_unhealthy">Starting to Affect Health</string>
    <string name="air_very_unhealthy">Affects Health</string>
    
    <!-- Debug -->
    <string name="current_language">Current Language: %1$s</string>
</resources>
```

# What I want
1. In flutter application, add a dropdown option that allow me to select language between `ไทย` and `Eng`. There should be an apply button to apply the change. This button should take a language string from the selected option from dropdown and apply it to the app.
2. There should be one variable in `main.dart`. Give it a name `lang` and it should store the language string value, `Eng` or `ไทย`. This variable is just a place holder for language setting in main application. It should be set to `Eng` as a default language.
3. From the code I will give you, you will see that this current implementation will accept lat and lon to update the widget. But with this new feature, it should accept language string too.
4. Android code (kotlin) should be the one that properly select and display the correct language for the widget. Currently, there is a `strings.xml` in both default `values` and `values-th` that contain all the words in both language (I think so), but currently, most of the text are hard coded. So you have to refactor the code that I gave you to use the string from `strings.xml` in `values` and `values-th` folders as I mentioned.
5. I did make change to `PM25Service.kt` to make it select the right portion from the API response since it contain both english and thai in the same response. You will have to refactor other code in this project to use this new `PM25Service.kt` correctly. There are 2 method that you can use `PM25Service.fetchPM25Data` and `PM25Service.getCachedData`. I will provide the example and how to use it below. Please note that the given example of these 2 mthods is not related the code from my app that I will show you. This example just demonstrate how to use them only
6. logging could still be in english. This feature is only include what will display to the user.
7. Display current language setting on application for debug purpose.
8. When changing the language, UI will have to reload with the new language applied, but the API response will have to be update too


```kotlin
// data classs for the result from PM25Service
data class LocationData(
    val lat: Double,
    val lon: Double,
    val tbIdn: Int,
    val tambon: String,
    val amphoe: String,
    val province: String
)

data class PM25Data(
    val pmCurrent: Double?,
    val hourlyReadings: List<Pair<String, Double>>?,
    val dateTimeString: String?,
    val location: LocationData?
)
```


```kotlin
// When you want to fetch a new data
val pm25Data = PM25Service.fetchPM25Data(
    latitude = 13.756331, 
    longitude = 100.501765,
    languageCode = "th" // For Thai language
)
```

```kotlin
// When you want to avoid network calls and use cached data if available
val languageCode = "en" // Define language once for consistency
val cachedData = PM25Service.getCachedData(languageCode = languageCode)

if (cachedData != null) {
    // Use cached data including location
    cachedData.location?.let { location ->
        // You can access original coordinates used to fetch this data
        Log.d("This data was fetched for location at: ${location.lat}, ${location.lon}")
        
        // Display location name
        locationNameTextView.text = "${location.tambon}, ${location.amphoe}, ${location.province}"
        
        // You could check if user's current location is still close to the cached data location
        val currentUserLocation = getCurrentUserLocation()
        val distanceToDataLocation = calculateDistance(
            currentUserLocation.latitude, currentUserLocation.longitude,
            location.lat, location.lon
        )
        
        if (distanceToDataLocation > 5000) { // More than 5km away
            // Maybe fetch fresh data instead
            showMessage("Your location has changed significantly, fetching updated air quality data...")
        }
    }
} else {
    // Need to fetch fresh data
    lifecycleScope.launch {
        val freshData = PM25Service.fetchPM25Data(
            latitude, 
            longitude,
            languageCode = languageCode
        )
        updateUI(freshData)
    }
}
```

# Approach to implement the language switching feature
1. create a Method Channel to communicate between Flutter and Android native code, explicitly defining both English and Thai language resources in separate folders (I did that already for `string.xml`), and mapping Flutter's language identifiers to Android locale codes. The reason we need to map flutter language code to android locale becasue in flutter application I have my custom language changing method, but when it come to android side, I want to use the Android locale codes. So influtter, "Eng" map to Locale("en"), and "ไทย" map to Locale("th")
2. `strings.xml` from the default `values` folder will store string in English as a default one, and `values-th` folders will contain `strings.xml` for Thai text.
3. Define a constant for the language channel name `languageChannel` in `lib\constants\app_constants.dart`
4. Sets up the communication channel between Flutter and Android by create a method channel handler in MainActivity `configureFlutterEngine(flutterEngine: FlutterEngine)`. This method should initialize method channel and set call handler
5. Create a language mapping method to converts Flutter language codes to Android Locale objects. `mapFlutterLanguageToLocale(flutterLanguage: String): Locale`. It should accept the string value ("Eng" or "ไทย") and output a locale object (Locale("en") or Locale("th"))
6. Create a locale update method to updates the app's language configuration, saves preference, recreates Activity. `updateLocale(locale: Locale)`
7. Create an initialization method (`initializeLanguage()`) to loads saved language preference on app startup. It should be called from `onCreate()` method of MainActivity. It should retrieves language from SharedPreferences, calls `updateLocale()`
8. The Flutter side will send the selected language ("Eng" or "ไทย") to Android. Values "Eng" and "ไทย" are used in Flutter's UI components. When language is changed in Flutter, it calls the method channel with the selected value

# To clarify
1. Method Channel Handler
1.1. Receives calls from Flutter with method name and arguments
1.2. For "changeLanguage" method:
1.2.1. Extracts language String from arguments ("Eng" or "ไทย")
1.2.2. Maps language to proper Locale using mapFlutterLanguageToLocale()
1.2.3. Calls updateLocale() with the mapped Locale
1.2.4. Returns success result to Flutter
2. Language Mapping Method
2.1 Takes input String from Flutter ("Eng" or "ไทย")
2.2 Returns appropriate Locale object:
2.2.1 "Eng" → Locale("en")
2.2.2 "ไทย" → Locale("th")
2.2.3 Any other value → Locale("en") as default
3. Locale Update Method
3.1. Takes a Locale object
3.2. Sets this locale as default using Locale.setDefault()
3.3. Updates the Configuration with new locale
3.4. Persists language preference to SharedPreferences
3.5. Recreates the activity to apply changes
4. Language Initialization Method
4.1 Reads SharedPreferences to get stored language code
4.2 Defaults to "en" if no language is stored
4.3 Creates appropriate Locale object
4.4 alls updateLocale() with this Locale

This is just a draft from my mind. It may have some error or something that could be improve. You have to think about the solution carefully for this feature, you have unlimited time for this task because the main goal is the correct, good, and flawless solution. If you don't know anything, ask me. Do not assume anything. I do not accept the answer or solution that come from your assumption. I can give you more information as you requested, then you can give me the solution for those in the next response. Then tell me how to implement this feature correctly. In the attached file, I will show you some part of my code that I think it is related to this task. If you need to see other part of my code, ask me, I will provide it to you.


`android\app\src\main\java\com\check_phoon\android_widget\service\WidgetUpdateForegroundService.kt`
```kotlin
package com.check_phoon.android_widget.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.check_phoon.android_widget.MainActivity
import com.check_phoon.android_widget.R
import com.check_phoon.android_widget.util.WidgetUtil
import com.check_phoon.android_widget.util.JobSchedulerHelper
import com.check_phoon.android_widget.widget.MyHomeMediumWidget
import com.check_phoon.android_widget.widget.MyHomeSmallWidget
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
        
        fun startService(context: Context) {
            // Only start if not already updating
            if (isUpdating.compareAndSet(false, true)) {
                val intent = Intent(context, WidgetUpdateForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "Service start requested")
            } else {
                Log.d(TAG, "Update already in progress, skipping")
            }
        }
    }
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started, updating widgets")
        
        // Show a foreground notification
        val notification = createNotification()
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
        
        // Get location data from HomeWidgetPlugin
        val widgetData = HomeWidgetPlugin.getData(applicationContext)
        val locationDataString = widgetData.getString("AppLocationData", null)
        
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
        
        if (latitude == null || longitude == null) {
            Log.e(TAG, "Failed to parse coordinates: $locationDataString")
            return
        }
        
        Log.d(TAG, "Fetching PM2.5 data for location: $latitude, $longitude")
        
        // Fetch PM2.5 data
        val pm25Data = PM25Service.fetchPM25Data(latitude, longitude)
        
        Log.d(TAG, "PM2.5 data received: ${pm25Data.pmCurrent ?: "No data"} μg/m³")
        
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
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Widget Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for updating air quality widgets"
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
            .setContentTitle("Updating Air Quality Widget")
            .setContentText("Fetching latest air quality data...")
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
```

`android\app\src\main\java\com\check_phoon\android_widget\util\WidgetUtil.kt`
```kotlin
package com.check_phoon.android_widget.util

import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import com.check_phoon.android_widget.R
import java.text.SimpleDateFormat
import java.util.*

object WidgetUtil {
    
    data class PM25Data(
        val pmCurrent: Double?,
        val hourlyReadings: List<Pair<String, Double>>?,
        val dateThai: String?
    )
    
    fun buildWidgetViews(context: Context, layoutId: Int, pm25Data: PM25Data): RemoteViews {
        val views = RemoteViews(context.packageName, layoutId)
        
        // Extract data from PM25Data object
        val pmCurrent = pm25Data.pmCurrent
        val hourlyData = pm25Data.hourlyReadings
        val dateThai = pm25Data.dateThai
        
        // Set PM2.5 value
        views.setTextViewText(R.id.text_pm25, pmCurrent?.let { String.format("%.0f", it) } ?: "No data")
        
        // Add debug timestamp - current time in hh:mm:ss format
        val currentTime = getCurrentTimeFormatted()
        
        // Determine background, images, and text color based on PM2.5 value
        val (backgroundResId, humanImage, textColor, message) = when {
            pmCurrent == null -> {
                Quadruple(R.drawable.andwidjet1, R.drawable.verygood, "#FFFFFF", "No data")
            }
            pmCurrent <= 15 -> {
                Quadruple(R.drawable.andwidjet1, R.drawable.verygood, "#FFFFFF", "อากาศดีมาก")
            }
            pmCurrent <= 25 -> {
                views.setImageViewResource(R.id.nearme_id, R.drawable.near_me_dark)
                Quadruple(R.drawable.andwidjet2, R.drawable.good, "#303C46", "อากาศดี")
            }
            pmCurrent <= 37.5 -> {
                views.setImageViewResource(R.id.nearme_id, R.drawable.near_me_dark)
                Quadruple(R.drawable.andwidjet3, R.drawable.medium, "#303C46", "ปานกลาง")
            }
            pmCurrent <= 75 -> {
                Quadruple(R.drawable.andwidjet4, R.drawable.bad, "#FFFFFF", "เริ่มมีผลต่อสุขภาพ")
            }
            else -> {
                Quadruple(R.drawable.andwidjet5, R.drawable.verybad, "#FFFFFF", "มีผลต่อสุขภาพ")
            }
        }
        
        // Apply widget UI configuration
        views.setImageViewResource(R.id.widget_background, backgroundResId)
        views.setImageViewResource(R.id.human_image, humanImage)
        views.setTextViewText(R.id.text_recomend, message)
        
        // Set text colors
        val colorParsed = Color.parseColor(textColor)
        views.setTextColor(R.id.text_recomend, colorParsed)
        views.setTextColor(R.id.text_pm25, colorParsed)
        views.setTextColor(R.id.text_pm25_unit, colorParsed)
        views.setTextColor(R.id.text_pm25_header, colorParsed)
        views.setTextColor(R.id.date_text, colorParsed)
        
        // Clean and set date text with debug timestamp
        // val cleanedDateThai = dateThai?.replace(Regex("(จันทร์|อังคาร|พุธ|พฤหัสบดี|ศุกร์|เสาร์|อาทิตย์)"), "")
        // val dateWithDebugTime = "${cleanedDateThai ?: "No date"} [${currentTime}]"
        // views.setTextViewText(R.id.date_text, dateWithDebugTime)

        val cleanedDateThai = dateThai?.replace(Regex("(จันทร์|อังคาร|พุธ|พฤหัสบดี|ศุกร์|เสาร์|อาทิตย์)"), "")
        views.setTextViewText(R.id.date_text, cleanedDateThai ?: "No date")

        // Add hourly readings
        views.removeAllViews(R.id.hourly_readings_container)
        if (!hourlyData.isNullOrEmpty()) {
            for ((hour, pm25) in hourlyData) {
                val hourlyView = RemoteViews(context.packageName, R.layout.hourly_reading).apply {
                    setTextViewText(R.id.hour_text, hour)
                    setTextColor(R.id.pm_text, colorParsed)
                    setTextColor(R.id.hour_text, colorParsed)
                    setTextViewText(R.id.pm_text, String.format("%.1f", pm25))
                }
                views.addView(R.id.hourly_readings_container, hourlyView)
            }
        } else {
            val noDataView = RemoteViews(context.packageName, R.layout.hourly_reading).apply {
                setTextViewText(R.id.hour_text, "No data")
                setTextViewText(R.id.pm_text, "No data")
            }
            views.addView(R.id.hourly_readings_container, noDataView)
        }
        
        return views
    }
    
    // Helper class for returning multiple values
    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    
    // Get current Thai time in HH:mm:ss format - with time zone caching for efficiency
    fun getCurrentTimeFormatted(): String {
        val date = Date()  // Current time
        // Use static formatter for better performance
        return timeFormatter.format(date)
    }

    // Add this static formatter at the class level
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Bangkok")  // Set to Thai time
    }
}
```

`android\app\src\main\java\com\check_phoon\android_widget\widget\BaseWidgetProvider.kt`
```kotlin
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
```

`android\app\src\main\kotlin\com\check_phoon\android_widget\MainActivity.kt`
```kotlin
package com.check_phoon.android_widget

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.content.Context
import io.flutter.plugins.GeneratedPluginRegistrant

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.check_phoon.android_widget/battery_optimization"
    
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine)
        
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "isIgnoringBatteryOptimizations" -> {
                    result.success(isIgnoringBatteryOptimizations())
                }
                "requestIgnoreBatteryOptimizations" -> {
                    requestIgnoreBatteryOptimizations()
                    result.success(true)
                }
                else -> result.notImplemented()
            }
        }
    }
    
    private fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
    }
    
    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations()) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}
```

`android\app\src\main\res\layout\my_home_medium_widget.xml`
```xml
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    style="@style/Widget.Android.AppWidget.Container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="@dimen/widget_margin"
    android:theme="@style/Theme.Android.AppWidgetContainer">


    <!-- Background Image -->
    <ImageView
        android:id="@+id/widget_background"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="centerCrop"
        android:src="@drawable/andwidjet1"
        android:alpha="0.7" />

    <!-- Main Content Container -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:orientation="horizontal"
        android:paddingHorizontal="16dp">

        <!-- First Column (Hourly Readings) -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:gravity="center"
            android:orientation="vertical">

            <LinearLayout
                android:id="@+id/pm25_header"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:orientation="horizontal">

                <TextView
                    android:id="@+id/text_pm25_header"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/pm25_hourly"
                    android:textColor="#FFFFFF"
                    android:textSize="14sp"
                    android:textStyle="normal"
                    />

                <ImageView
                    android:id="@+id/nearme_id"
                    android:layout_width="wrap_content"
                    android:layout_height="15dp"
                    android:layout_marginStart="4dp"
                    android:src="@drawable/near_me" />
            </LinearLayout>

            <LinearLayout
                android:id="@+id/pm25_value_section"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <ImageView
                    android:id="@+id/human_image"
                    android:layout_width="40dp"
                    android:layout_height="40dp"
                    android:adjustViewBounds="true"
                    android:src="@drawable/verygood" />

                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="8dp"
                    android:orientation="vertical">

                    <LinearLayout
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:gravity="bottom"
                        android:orientation="horizontal">

                        <TextView
                            android:id="@+id/text_pm25"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="000"
                            android:textColor="#FFFFFF"
                            android:textSize="28sp"
                            android:textStyle="bold"
                            />


                        <TextView
                            android:id="@+id/text_pm25_unit"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/pm25_unit"
                            android:textColor="#FFFFFF"
                            android:textSize="8sp"
                            android:textStyle="normal"
                              />
                    </LinearLayout>

                    <TextView
                        android:id="@+id/text_recomend"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="เริ่มมีผลต่อสุขภาพ"
                        android:textColor="#FFFFFF"
                        android:textSize="14sp"
                        android:textStyle="normal"
                        />
                </LinearLayout>
            </LinearLayout>
        </LinearLayout>

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:gravity="center"
            android:orientation="vertical">

            <TextView
                android:id="@+id/date_text"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"

                android:text="วันที่ 3 มีนาคม 2568"
                android:textColor="#FFFFFF"
                android:textSize="12sp"
                android:textStyle="normal" />

            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:padding="10dp">

                <!-- Container for hourly readings -->
                <LinearLayout
                    android:id="@+id/hourly_readings_container"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal" />
            </LinearLayout>
        </LinearLayout>

        <!-- Second Column (PM2.5 Data) -->

    </LinearLayout>
</RelativeLayout>

```

`android\app\src\main\res\layout\my_home_small_widget.xml`
```xml
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    style="@style/Widget.Android.AppWidget.Container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="@dimen/widget_margin"
    android:theme="@style/Theme.Android.AppWidgetContainer">

    <!-- Background Image -->
    <ImageView
        android:id="@+id/widget_background"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="centerCrop"
        android:src="@drawable/andwidjet1"
        android:alpha="0.7" />

    <!-- Main Content Container -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:orientation="horizontal"
        android:paddingHorizontal="16dp">

        <!-- First Column (Hourly Readings) -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:gravity="center"
            android:orientation="vertical">

            <LinearLayout
                android:id="@+id/pm25_header"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:orientation="horizontal">

                <TextView
                    android:id="@+id/text_pm25_header"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/pm25_hourly"
                    android:textColor="#FFFFFF"
                    android:textSize="14sp"
                    android:textStyle="normal"
                    />

                <ImageView
                    android:id="@+id/nearme_id"
                    android:layout_width="wrap_content"
                    android:layout_height="15dp"
                    android:layout_marginStart="4dp"
                    android:src="@drawable/near_me" />
            </LinearLayout>

            <LinearLayout
                android:id="@+id/pm25_value_section"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <ImageView
                    android:id="@+id/human_image"
                    android:layout_width="40dp"
                    android:layout_height="40dp"
                    android:adjustViewBounds="true"
                    android:src="@drawable/verygood" />

                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="8dp"
                    android:orientation="vertical">

                    <LinearLayout
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:gravity="bottom"
                        android:orientation="horizontal">

                        <TextView
                            android:id="@+id/text_pm25"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="000"
                            android:textColor="#FFFFFF"
                            android:textSize="28sp"
                            android:textStyle="bold"
                            />


                        <TextView
                            android:id="@+id/text_pm25_unit"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/pm25_unit"
                            android:textColor="#FFFFFF"
                            android:textSize="8sp"
                            android:textStyle="normal"
                          />
                    </LinearLayout>

                    <TextView
                        android:id="@+id/text_recomend"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="เริ่มมีผลต่อสุขภาพ"
                        android:textColor="#FFFFFF"
                        android:textSize="14sp"
                        android:textStyle="normal"
                      />
                </LinearLayout>
            </LinearLayout>
        </LinearLayout>


        <!-- Second Column (PM2.5 Data) -->

    </LinearLayout>
</RelativeLayout>

```

`lib\constants\app_constants.dart`
```dart
class AppConstants {
  // App Group ID for widget communication
  static const String appGroupId = "group.homescreenaapp";
  
  // Widget names
  static const String iOSWidgetName = "MyHomeWidget";
  static const String androidMediumWidgetName = "widget.MyHomeMediumWidget";
  static const String androidSmallWidgetName = "widget.MyHomeSmallWidget";
  
  // Data keys
  static const String appLocationData = "AppLocationData";

  // Method channels
  static const String batteryOptChannel = "com.check_phoon.android_widget/battery_optimization";
}
```

`lib\screens\location_page.dart`
```dart
import 'package:flutter/material.dart';
import '../services/location_service.dart';
import '../services/widget_service.dart';

class LocationPage extends StatefulWidget {
  @override
  _LocationPageState createState() => _LocationPageState();
}

class _LocationPageState extends State<LocationPage> with WidgetsBindingObserver {
  final LocationService _locationService = LocationService();
  final WidgetService _widgetService = WidgetService();
  String locationMessage = "Press the button to get location";
  
  // Track if we've updated widgets in this session
  bool _hasUpdatedWidgetsThisSession = false;

  @override
  void initState() {
    super.initState();
    // Register as an observer for app lifecycle changes
    WidgetsBinding.instance.addObserver(this);
    Future.microtask(() => _initServices());
  }

  @override
  void dispose() {
    // Unregister observer when the page is disposed
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  // This method is called whenever the app lifecycle state changes
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && !_hasUpdatedWidgetsThisSession) {
      // App has come to the foreground - update widgets once per session
      print('App resumed - updating widgets');
      _getCurrentLocation();
      _hasUpdatedWidgetsThisSession = true;
    } else if (state == AppLifecycleState.paused) {
      // Reset the flag when app goes to background
      _hasUpdatedWidgetsThisSession = false;
    }
  }

  Future<void> _initServices() async {
    await _widgetService.initialize();
    _getCurrentLocation();
    _hasUpdatedWidgetsThisSession = true; // Mark as updated for this session
  }

  Future<void> _getCurrentLocation() async {
    try {
      final locationResult = await _locationService.getCurrentLocation();
      
      setState(() {
        if (locationResult.error != null) {
          locationMessage = locationResult.error!;
        } else if (locationResult.position != null) {
          locationMessage = "Latitude: ${locationResult.position!.latitude}, "
              "Longitude: ${locationResult.position!.longitude}";
          
          // Update widgets with new location
          _widgetService.updateWidgetsWithLocation(
            locationResult.position!.latitude, 
            locationResult.position!.longitude
          );
        }
      });
    } catch (e) {
      setState(() {
        locationMessage = "Error getting location: $e";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("Get Current Location")),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(locationMessage, textAlign: TextAlign.center),
            SizedBox(height: 20),
            ElevatedButton(
              onPressed: _getCurrentLocation,
              child: Text("Get Location"),
            ),
            SizedBox(height: 20),
            ElevatedButton(
              onPressed: () async {
                // Force immediate widget update
                await _widgetService.forceWidgetUpdate();
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('Widgets updated manually'))
                );
              },
              child: Text("Force Update Widgets"),
            ),
          ],
        ),
      ),
    );
  }
}
```

`lib\services\widget_service.dart`
```dart
import 'dart:io' show Platform;
import 'package:home_widget/home_widget.dart';
import '../constants/app_constants.dart';
import 'package:geolocator/geolocator.dart';

class WidgetService {
  Future<void> initialize() async {
    await HomeWidget.setAppGroupId(AppConstants.appGroupId);
  }

  Future<void> updateWidgetsWithLocation(double latitude, double longitude) async {
    String data = "$latitude,$longitude";
    
    try {
      // Create a list to hold our futures
      List<Future> futures = [];
      
      // Always save the widget data
      futures.add(HomeWidget.saveWidgetData(AppConstants.appLocationData, data));
      
      // Add platform-specific widget updates
      if (Platform.isIOS) {
        futures.add(HomeWidget.updateWidget(iOSName: AppConstants.iOSWidgetName));
      } else if (Platform.isAndroid) {
        futures.add(HomeWidget.updateWidget(androidName: AppConstants.androidMediumWidgetName));
        futures.add(HomeWidget.updateWidget(androidName: AppConstants.androidSmallWidgetName));
      }
      
      // Now wait for all the futures to complete
      await Future.wait(futures);
      
      print("Widgets updated with location: $data at ${DateTime.now().toLocal()}");
    } catch (e) {
      print("Error updating widgets: $e");
    }
  }

  /// Force update widgets with last saved location
  Future<void> forceWidgetUpdate() async {
    try {
      // Update widgets for the current platform only
      if (Platform.isIOS) {
        // Update iOS widget
        await HomeWidget.updateWidget(
          iOSName: AppConstants.iOSWidgetName
        );
      } else if (Platform.isAndroid) {
        // Update Android widgets
        await HomeWidget.updateWidget(
          androidName: AppConstants.androidMediumWidgetName
        );
        await HomeWidget.updateWidget(
          androidName: AppConstants.androidSmallWidgetName
        );
      }
      
      print("Widgets force updated at ${DateTime.now().toLocal()}");
    } catch (e) {
      print("Error force updating widgets: $e");
    }
  }

  /// Get last saved location and update widgets with it
  Future<void> updateWithLastLocation() async {
    try {
      // Try to get the last location from Geolocator
      Position? lastPosition = await Geolocator.getLastKnownPosition();
      
      // If we have a last position, use it to update the widgets
      if (lastPosition != null) {
        await updateWidgetsWithLocation(
          lastPosition.latitude, 
          lastPosition.longitude
        );
      } else {
        // If no last position, just force update with whatever data is stored
        await forceWidgetUpdate();
      }
    } catch (e) {
      print("Error updating with last location: $e");
      // Fall back to force update if there's an error
      await forceWidgetUpdate();
    }
  }
}
```

`lib\main.dart`
```dart
// lib/main.dart
import 'package:flutter/material.dart';
import 'package:home_widget/home_widget.dart';
import 'screens/location_page.dart';
import 'constants/app_constants.dart';
import 'services/widget_service.dart';
import 'dart:io';
import 'package:flutter/services.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Set up HomeWidget and trigger immediate widget update
  HomeWidget.setAppGroupId(AppConstants.appGroupId);
  
  // Request battery optimization exception on Android
  if (Platform.isAndroid) {
    _requestBatteryOptimizationExemption();
  }
  
  // Initialize the app
  runApp(const MyApp());
  
  // Update widgets when app starts
  _updateWidgetsOnAppStart();
}

Future<void> _requestBatteryOptimizationExemption() async {
  try {
    const platform = MethodChannel(AppConstants.batteryOptChannel);
    final bool isIgnoring = await platform.invokeMethod('isIgnoringBatteryOptimizations');
    
    if (!isIgnoring) {
      final bool requested = await platform.invokeMethod('requestIgnoreBatteryOptimizations');
      print('Battery optimization exemption requested: $requested');
    } else {
      print('Already ignoring battery optimizations');
    }
  } catch (e) {
    print('Error with battery optimization: $e');
  }
}

Future<void> _updateWidgetsOnAppStart() async {
  // Create WidgetService instance and request an immediate update
  final widgetService = WidgetService();
  await widgetService.initialize();
  
  // Force widgets to update on both platforms
  await HomeWidget.updateWidget(
    iOSName: AppConstants.iOSWidgetName,
    androidName: AppConstants.androidMediumWidgetName
  );
  
  await HomeWidget.updateWidget(
    androidName: AppConstants.androidSmallWidgetName
  );
  
  print('Widgets updated on app start');
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false, 
      home: LocationPage()
    );
  }
}
```







```kotlin
package com.example.test_wid_and.util

import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import com.example.test_wid_and.R
import java.text.SimpleDateFormat
import java.util.*

object WidgetUtil {
    
    data class PM25Data(
        val pmCurrent: Double?,
        val hourlyReadings: List<Pair<String, Double>>?,
        val dateThai: String?
    )
    
    fun buildWidgetViews(context: Context, layoutId: Int, pm25Data: PM25Data): RemoteViews {
        // Get all string resources at once to avoid multiple context recreations
        val resources = LanguageHelper.getLocalizedResources(context)
        val noDataText = resources.getString(R.string.no_data)
        val veryGoodText = resources.getString(R.string.air_very_good)
        val goodText = resources.getString(R.string.air_good)
        val moderateText = resources.getString(R.string.air_moderate)
        val unhealthyText = resources.getString(R.string.air_unhealthy)
        val veryUnhealthyText = resources.getString(R.string.air_very_unhealthy)
        val pm25HourlyText = resources.getString(R.string.pm25_hourly)
        val pm25UnitText = resources.getString(R.string.pm25_unit)
        
        val views = RemoteViews(context.packageName, layoutId)
        
        // Extract data from PM25Data object
        val pmCurrent = pm25Data.pmCurrent
        val hourlyData = pm25Data.hourlyReadings
        val dateText = pm25Data.dateThai
        
        // Set PM2.5 value
        views.setTextViewText(R.id.text_pm25, pmCurrent?.let { String.format("%.0f", it) } ?: noDataText)
        
        // Determine background, images, and text color based on PM2.5 value
        val (backgroundResId, humanImage, textColor, message) = when {
            pmCurrent == null -> {
                Quadruple(R.drawable.andwidjet1, R.drawable.verygood, "#FFFFFF", noDataText)
            }
            pmCurrent <= 15 -> {
                Quadruple(R.drawable.andwidjet1, R.drawable.verygood, "#FFFFFF", veryGoodText)
            }
            pmCurrent <= 25 -> {
                views.setImageViewResource(R.id.nearme_id, R.drawable.near_me_dark)
                Quadruple(R.drawable.andwidjet2, R.drawable.good, "#303C46", goodText)
            }
            pmCurrent <= 37.5 -> {
                views.setImageViewResource(R.id.nearme_id, R.drawable.near_me_dark)
                Quadruple(R.drawable.andwidjet3, R.drawable.medium, "#303C46", moderateText)
            }
            pmCurrent <= 75 -> {
                Quadruple(R.drawable.andwidjet4, R.drawable.bad, "#FFFFFF", unhealthyText)
            }
            else -> {
                Quadruple(R.drawable.andwidjet5, R.drawable.verybad, "#FFFFFF", veryUnhealthyText)
            }
        }
        
        // Apply widget UI configuration
        views.setImageViewResource(R.id.widget_background, backgroundResId)
        views.setImageViewResource(R.id.human_image, humanImage)
        views.setTextViewText(R.id.text_recomend, message)
        
        // Set text colors
        val colorParsed = Color.parseColor(textColor)
        views.setTextColor(R.id.text_recomend, colorParsed)
        views.setTextColor(R.id.text_pm25, colorParsed)
        views.setTextColor(R.id.text_pm25_unit, colorParsed)
        views.setTextColor(R.id.text_pm25_header, colorParsed)
        views.setTextColor(R.id.date_text, colorParsed)
        
        // Set PM2.5 header text
        views.setTextViewText(R.id.text_pm25_header, pm25HourlyText)
        
        // Set PM2.5 unit text
        views.setTextViewText(R.id.text_pm25_unit, pm25UnitText)
        
        // Clean and set date text
        views.setTextViewText(R.id.date_text, dateText ?: noDataText)
        
        // Add hourly readings
        views.removeAllViews(R.id.hourly_readings_container)
        if (!hourlyData.isNullOrEmpty()) {
            for ((hour, pm25) in hourlyData) {
                val hourlyView = RemoteViews(context.packageName, R.layout.hourly_reading).apply {
                    setTextViewText(R.id.hour_text, hour)
                    setTextColor(R.id.pm_text, colorParsed)
                    setTextColor(R.id.hour_text, colorParsed)
                    setTextViewText(R.id.pm_text, String.format("%.1f", pm25))
                }
                views.addView(R.id.hourly_readings_container, hourlyView)
            }
        } else {
            val noDataView = RemoteViews(context.packageName, R.layout.hourly_reading).apply {
                setTextViewText(R.id.hour_text, noDataText)
                setTextViewText(R.id.pm_text, noDataText)
            }
            views.addView(R.id.hourly_readings_container, noDataView)
        }
        
        return views
    }
    
    // Helper class for returning multiple values
    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    
    // Get current Thai time in HH:mm:ss format - with time zone caching for efficiency
    fun getCurrentTimeFormatted(): String {
        val date = Date()  // Current time
        // Use static formatter for better performance
        return timeFormatter.format(date)
    }

    // Add this static formatter at the class level
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Bangkok")  // Set to Thai time
    }
}
```

