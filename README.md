# test_wid_and

A new Flutter project.

## Getting Started

This project is a starting point for a Flutter application.

A few resources to get you started if this is your first Flutter project:

- [Lab: Write your first Flutter app](https://docs.flutter.dev/get-started/codelab)
- [Cookbook: Useful Flutter samples](https://docs.flutter.dev/cookbook)

For help getting started with Flutter development, view the
[online documentation](https://docs.flutter.dev/), which offers tutorials,
samples, guidance on mobile development, and a full API reference.





I have this test flutter app. The main feature I want to do here is to create a widget for both Android and iOS mobile phone. However, there are sone issues that I want you to fix on Android side. I will list them in the following:
1. There is an issue about a widget. When I test this on emulator, it update the value correctly based on 60 seconds interval I set. Even when I exited from app, it still update every 60 seconds (on emulator). But when I test this on real device, it does not update until I open an app.
2. The widget should be working even when an app closed. It should work and always be up to data periodically (every 60 seconds).
3. This is a flutter project with some native kotlin file. There is one main.dart file and other 2 kotlin file. I will show them below.
4. Refactore code into a folder and structure it professionally. The main goal is to make it easy to work with.
5. You have to tell me which files and directories I have to create for this.

`lib\main.dart`
```dart
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:home_widget/home_widget.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(debugShowCheckedModeBanner: false, home: LocationPage());
  }
}

class LocationPage extends StatefulWidget {
  @override
  _LocationPageState createState() => _LocationPageState();
}

class _LocationPageState extends State<LocationPage> {
  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    _getCurrentLocation();
    HomeWidget.setAppGroupId(appGroupId);
  }

  String appGroupId = "group.homescreenaapp";
  String iOSWidgetName = "MyHomeWidget";

  String dataKey = "locationData_from_flutter_APP_new_5";

  String locationMessage = "Press the button to get location";

  Future<void> _getCurrentLocation() async {
    bool serviceEnabled;
    LocationPermission permission;

    // Check if location services are enabled
    serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      setState(() {
        locationMessage = "Location services are disabled.";
      });
      return;
    }

    // Check location permission
    permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        setState(() {
          locationMessage = "Location permission denied.";
        });
        return;
      }
    }

    if (permission == LocationPermission.deniedForever) {
      setState(() {
        locationMessage = "Location permissions are permanently denied.";
      });
      return;
    }

    // Get current location
    Position position = await Geolocator.getCurrentPosition(
      desiredAccuracy: LocationAccuracy.high,
    );

    setState(() {
      locationMessage =
          "Latitude: ${position.latitude}, Longitude: ${position.longitude}";
    });
    var data = "${position.latitude},${position.longitude}";
    await HomeWidget.saveWidgetData(dataKey, data);
    await HomeWidget.updateWidget(
      iOSName: iOSWidgetName,
      androidName: "MyHomeMediumWidget",
    );
    await HomeWidget.updateWidget(androidName: "MyHomeSmallWidget");
    print(data);
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
          ],
        ),
      ),
    );
  }
}
```


`android\app\src\main\java\com\example\test_wid_and\MyHomeMediumWidget.kt`
```kotlin
package com.example.test_wid_and


import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.widget.RemoteViews

import es.antonborri.home_widget.HomeWidgetPlugin
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.round
import java.util.*

/**
 * Implementation of App Widget functionality.
 */
class MyHomeMediumWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            com.example.test_wid_and.MyHomeMediumWidget.FetchPM25Task(
                context,
                appWidgetManager,
                appWidgetId
            ).execute()

            // Schedule periodic updates every 1 minute
            val intent = Intent(context, MyHomeMediumWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)

            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                60000, // 1
                pendingIntent
            )

        }
    }

    private class FetchPM25Task(
        val context: Context,
        val appWidgetManager: AppWidgetManager,
        val appWidgetId: Int
    ) : AsyncTask<Void, Void, Triple<Double?, List<Pair<String, Double>>?, String?>?>() {


        override fun doInBackground(vararg params: Void?): Triple<Double?, List<Pair<String, Double>>?, String?>? {

            val date = Date()  // Current time
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("Asia/Bangkok")  // Set to Thai time
            val currentTimeThai = sdf.format(date)

            println(currentTimeThai)  // e.g. 20:17:53

            val widgetData = HomeWidgetPlugin.getData(context)
            val textFromFlutterApp = widgetData.getString("locationData_from_flutter_APP_new_5", null)
            val locationData = textFromFlutterApp ?: "13.00,100.0"
            val parts = locationData.split(",")
            return try {

                val latitude = parts.get(0)
                val longitude = parts.get(1).toDouble()
                val url = URL("https://pm25.gistda.or.th/rest/pred/getPm25byLocation?lat=$latitude&lng=$longitude")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    val dataObject = jsonObject.getJSONObject("data")

                    // Extract current PM2.5
                    val pmCurrent = dataObject.getJSONArray("pm25").getDouble(0)

                    // Extract Thai date information
                    val datetimeThai = dataObject.getJSONObject("datetimeThai")
                    val dateThaiFirst = datetimeThai.getString("dateThai")
                    val timeThai = datetimeThai.getString("timeThai")
//                    val timeThaiClean = timeThai.replace("เวลา", "").trim()
                    val dateThai = "$dateThaiFirst\n$timeThai"



                    // Extract hourly PM2.5 values and times
                    val hourlyData = dataObject.getJSONArray("graphPredictByHrs")
                    val hourlyReadings = mutableListOf<Pair<String, Double>>()
                    for (i in 0 until hourlyData.length()) {
                        val hourlyItem = hourlyData.getJSONArray(i)
                        val pm25Value = hourlyItem.getDouble(0)
                        val time = hourlyItem.getString(1)
                        // Extract the time in 24-hour format (HH:mm)
                        val date =
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                        val formattedTime =
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date.parse(time))
                        hourlyReadings.add(Pair(formattedTime, pm25Value))
                    }
                    Triple(pmCurrent, hourlyReadings, currentTimeThai)
                } else {
                    Triple(null, null, locationData)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Triple(null, null, locationData)
            }
        }

        override fun onPostExecute(result: Triple<Double?, List<Pair<String, Double>>?, String?>?) {
            val views = RemoteViews(context.packageName, R.layout.my_home_medium_widget)

            // ดึงค่าผลลัพธ์จาก Triple
            val pmCurrent = result?.first
            val hourlyData = result?.second
            val dateThai = result?.third

            // อัปเดตค่า PM2.5 ปัจจุบัน
            views.setTextViewText(R.id.text_pm25, pmCurrent?.let { String.format("%.0f", it) } ?: "No data")
// Set background image and message based on pmCurrent value
            val backgroundResId: Int
            val nearmeId: Int
            val humanImage: Int
            val message: String
            var textColor: String

            when {
                pmCurrent == null -> {
                    backgroundResId = R.drawable.andwidjet1
                    humanImage = R.drawable.verygood
                    textColor = "#FFFFFF" // red color in hex
                    message = "No data"
                }
                pmCurrent <= 15 -> {
                    backgroundResId = R.drawable.andwidjet1 // Image for "อากาศดีมาก"
                    humanImage = R.drawable.verygood
                    textColor = "#FFFFFF" // green color for good air quality
                    message = "อากาศดีมาก"
                }
                pmCurrent <= 25 -> {
                    backgroundResId = R.drawable.andwidjet2 // Image for "อากาศดี"
                    humanImage = R.drawable.good
                    nearmeId = R.drawable.near_me_dark
                    views.setImageViewResource(R.id.nearme_id, nearmeId)
                    textColor = "#303C46" // green color for good air quality

                    message = "อากาศดี"
                }
                pmCurrent <= 37.5 -> {
                    backgroundResId = R.drawable.andwidjet3 // Image for "อากาศปานกลาง"
                    humanImage = R.drawable.medium
                    nearmeId = R.drawable.near_me_dark
                    views.setImageViewResource(R.id.nearme_id, nearmeId)
                    textColor = "#303C46" // yellow color for moderate air quality
                    message = "ปานกลาง"

                }
                pmCurrent <= 75 -> {
                    backgroundResId = R.drawable.andwidjet4 // Image for "เริ่มมีผลต่อสุขภาพ"
                    humanImage = R.drawable.bad
                    textColor = "#FFFFFF" // orange color for unhealthy air
                    message = "เริ่มมีผลต่อสุขภาพ"
                }
                else -> {
                    backgroundResId = R.drawable.andwidjet5 // Image for "มีผลต่อสุขภาพ"
                    humanImage = R.drawable.verybad
                    textColor = "#FFFFFF" // red color for very unhealthy air
                    message = "มีผลต่อสุขภาพ"
                }
            }

// Set the background image and the message
            views.setImageViewResource(R.id.widget_background, backgroundResId)
            views.setImageViewResource(R.id.human_image, humanImage)
            views.setTextViewText(R.id.text_recomend, message)
// Set text color programmatically
            views.setTextColor(R.id.text_recomend, Color.parseColor(textColor))
            views.setTextColor(R.id.text_pm25, Color.parseColor(textColor))
            views.setTextColor(R.id.text_pm25_unit, Color.parseColor(textColor))
            views.setTextColor(R.id.text_pm25_header, Color.parseColor(textColor))
            views.setTextColor(R.id.date_text, Color.parseColor(textColor))


            views.setImageViewResource(R.id.widget_background, backgroundResId)
            // อัปเดตวันที่ไทย ถ้ามี
            // ใช้ Regex เพื่อลบชื่อวัน (จันทร์ - อาทิตย์) ออกจาก dateThai
            val cleanedDateThai = dateThai?.replace(Regex("(จันทร์|อังคาร|พุธ|พฤหัสบดี|ศุกร์|เสาร์|อาทิตย์)"), "")

            // ตั้งค่าข้อความใน Widget
            views.setTextViewText(R.id.date_text, cleanedDateThai ?: "No date")


            // ล้างข้อมูลเก่าของ hourly_readings_container
            views.removeAllViews(R.id.hourly_readings_container)

            if (!hourlyData.isNullOrEmpty()) {
                for ((hour, pm25) in hourlyData) {
                    val hourlyView = RemoteViews(context.packageName, R.layout.hourly_reading).apply {
                        setTextViewText(R.id.hour_text, hour)
                        setTextColor(R.id.pm_text, Color.parseColor(textColor))
                        setTextColor(R.id.hour_text, Color.parseColor(textColor))
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

            // อัปเดต Widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

    }
}
```

`android\app\src\main\java\com\example\test_wid_and\MyHomeSmallWidget.kt`
```kotlin
package com.example.test_wid_and
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.widget.RemoteViews
import es.antonborri.home_widget.HomeWidgetPlugin
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Implementation of App Widget functionality.
 */
class MyHomeSmallWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            FetchPM25Task(context, appWidgetManager, appWidgetId).execute()

            // Schedule periodic updates every 1 minute
            val intent = Intent(context, MyHomeSmallWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)

            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                600000, // 10 minutes
                pendingIntent
            )

        }
    }

    private class FetchPM25Task(
        val context: Context,
        val appWidgetManager: AppWidgetManager,
        val appWidgetId: Int
    ) : AsyncTask<Void, Void, Triple<Double?, List<Pair<String, Double>>?, String?>?>() {


        override fun doInBackground(vararg params: Void?): Triple<Double?, List<Pair<String, Double>>?, String?>? {
            return try {
                val widgetData = HomeWidgetPlugin.getData(context)
                val textFromFlutterApp = widgetData.getString("locationData_from_flutter_APP_new_5", null)
                val parts = textFromFlutterApp?.split(",")
                val latitude = parts?.get(0)?.toDouble()
                val longitude = parts?.get(1)?.toDouble()
                val url = URL("https://pm25.gistda.or.th/rest/pred/getPm25byLocation?lat=$latitude&lng=$longitude")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    val dataObject = jsonObject.getJSONObject("data")

                    // Extract current PM2.5
                    val pmCurrent = dataObject.getJSONArray("pm25").getDouble(0)

                    // Extract Thai date information
                    val datetimeThai = dataObject.getJSONObject("datetimeThai")
                    val dateThai = datetimeThai.getString("dateThai")
                    val timeThai = datetimeThai.getString( "timeThai")

                    // Extract hourly PM2.5 values and times
                    val hourlyData = dataObject.getJSONArray("graphPredictByHrs")
                    val hourlyReadings = mutableListOf<Pair<String, Double>>()
                    for (i in 0 until hourlyData.length()) {
                        val hourlyItem = hourlyData.getJSONArray(i)
                        val pm25Value = hourlyItem.getDouble(0)
                        val time = hourlyItem.getString(1)
                        // Extract the time in 24-hour format (HH:mm)
                        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                        val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date.parse(time))
                        hourlyReadings.add(Pair(formattedTime, pm25Value))
                    }
                    Triple(pmCurrent, hourlyReadings, dateThai)
                } else {
                    Triple(null, null, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Triple(null, null, null)
            }
        }

        override fun onPostExecute(result: Triple<Double?, List<Pair<String, Double>>?, String?>?) {
            val views = RemoteViews(context.packageName, R.layout.my_home_small_widget)


            // ดึงค่าผลลัพธ์จาก Triple
            val pmCurrent = result?.first?.roundToInt()
            val hourlyData = result?.second
            val dateThai = result?.third

            // อัปเดตค่า PM2.5 ปัจจุบัน
            views.setTextViewText(R.id.text_pm25, pmCurrent?.toString() ?: "No data")
            val backgroundResId: Int
            val nearmeId: Int
            val humanImage: Int
            val message: String
            var textColor: String

            when {
                pmCurrent == null -> {
                    backgroundResId = R.drawable.andwidjet1
                    humanImage = R.drawable.verygood
                    textColor = "#FFFFFF" // red color in hex
                    message = "No data"
                }
                pmCurrent <= 15 -> {
                    backgroundResId = R.drawable.andwidjet1 // Image for "อากาศดีมาก"
                    humanImage = R.drawable.verygood
                    textColor = "#FFFFFF" // green color for good air quality
                    message = "อากาศดีมาก"
                }
                pmCurrent <= 25 -> {
                    backgroundResId = R.drawable.andwidjet2 // Image for "อากาศดี"
                    humanImage = R.drawable.good
                    nearmeId = R.drawable.near_me_dark
                    views.setImageViewResource(R.id.nearme_id, nearmeId)
                    textColor = "#303C46" // green color for good air quality

                    message = "อากาศดี"
                }
                pmCurrent <= 37.5 -> {
                    backgroundResId = R.drawable.andwidjet3 // Image for "อากาศปานกลาง"
                    humanImage = R.drawable.medium
                    nearmeId = R.drawable.near_me_dark
                    views.setImageViewResource(R.id.nearme_id, nearmeId)
                    textColor = "#303C46" // yellow color for moderate air quality
                    message = "ปานกลาง"

                }
                pmCurrent <= 75 -> {
                    backgroundResId = R.drawable.andwidjet4 // Image for "เริ่มมีผลต่อสุขภาพ"
                    humanImage = R.drawable.bad
                    textColor = "#FFFFFF" // orange color for unhealthy air
                    message = "เริ่มมีผลต่อสุขภาพ"
                }
                else -> {
                    backgroundResId = R.drawable.andwidjet5 // Image for "มีผลต่อสุขภาพ"
                    humanImage = R.drawable.verybad
                    textColor = "#FFFFFF" // red color for very unhealthy air
                    message = "มีผลต่อสุขภาพ"
                }
            }

// Set the background image and the message
            views.setImageViewResource(R.id.widget_background, backgroundResId)
            views.setImageViewResource(R.id.human_image, humanImage)
            views.setTextViewText(R.id.text_recomend, message)
// Set text color programmatically
            views.setTextColor(R.id.text_recomend, Color.parseColor(textColor))
            views.setTextColor(R.id.text_pm25, Color.parseColor(textColor))
            views.setTextColor(R.id.text_pm25_unit, Color.parseColor(textColor))
            views.setTextColor(R.id.text_pm25_header, Color.parseColor(textColor))
            views.setTextColor(R.id.date_text, Color.parseColor(textColor))


            views.setImageViewResource(R.id.widget_background, backgroundResId)
            // อัปเดตวันที่ไทย ถ้ามี
            // ใช้ Regex เพื่อลบชื่อวัน (จันทร์ - อาทิตย์) ออกจาก dateThai
            val cleanedDateThai = dateThai?.replace(Regex("(จันทร์|อังคาร|พุธ|พฤหัสบดี|ศุกร์|เสาร์|อาทิตย์)"), "")

            // ตั้งค่าข้อความใน Widget
            views.setTextViewText(R.id.date_text, cleanedDateThai ?: "No date")


            // ล้างข้อมูลเก่าของ hourly_readings_container
            views.removeAllViews(R.id.hourly_readings_container)

            if (!hourlyData.isNullOrEmpty()) {
                for ((hour, pm25) in hourlyData) {
                    val hourlyView = RemoteViews(context.packageName, R.layout.hourly_reading).apply {
                        setTextViewText(R.id.hour_text, hour)
                        setTextColor(R.id.pm_text, Color.parseColor(textColor))
                        setTextColor(R.id.hour_text, Color.parseColor(textColor))
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

            // อัปเดต Widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

    }


}
```










I have a flutter project code with native kotlin integrated code. This is a test application that dedicated to one feature; the widget for air quality monitoring application. Now the widget is working correctly as it should be. It will always update data every 15 minutes (around that). When I open an application, it will force update right away. Even when I closed or killed an app, the widget will still working. But I have a task for you in the following
1. However, the code is getting messy and I solve bug and work on it, some part of the code may not be use anymore. I want you to check all of it and clean it. Basically clean and refactor is nescessary.
2. I want to set an update interval to 15 minutes, reguardless when app is closed or in standby state. But only do instant update when user open an app (just update it once each time user enter an app, do not make it update every second while user is in the app)
3. Battery and resource optimization is a main goal here, but the widget must still function as I mentioned. 
I will show you the code I have below. When you give me a solution, label each code snipped which file I should update (or create) too.

`android\app\src\main\java\com\example\test_wid_and\receiver\SystemEventReceiver.kt`
```kotlin
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
                Log.d(TAG, "Device booted, restarting widget updates")
                JobSchedulerHelper.scheduleWidgetUpdateJob(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "App updated, restarting widget updates")
                JobSchedulerHelper.scheduleWidgetUpdateJob(context)
            }
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.d(TAG, "Time/timezone changed, updating widgets")
                JobSchedulerHelper.runImmediateWidgetUpdate(context)
            }
        }
    }
}
```


`android\app\src\main\java\com\example\test_wid_and\service\PM25Service.kt`
```kotlin
// android/app/src/main/java/com/example/test_wid_and/service/PM25Service.kt
package com.example.test_wid_and.service

import com.example.test_wid_and.util.WidgetUtil.PM25Data
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class PM25Service {
    companion object {
        suspend fun fetchPM25Data(latitude: Double, longitude: Double): PM25Data {
            return try {
                val url = URL("https://pm25.gistda.or.th/rest/pred/getPm25byLocation?lat=$latitude&lng=$longitude")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    parseResponse(response)
                } else {
                    PM25Data(null, null, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                PM25Data(null, null, null)
            }
        }
        
        private fun parseResponse(response: String): PM25Data {
            val jsonObject = JSONObject(response)
            val dataObject = jsonObject.getJSONObject("data")
            
            // Extract current PM2.5
            val pmCurrent = dataObject.getJSONArray("pm25").getDouble(0)
            
            // Extract Thai date information
            val datetimeThai = dataObject.getJSONObject("datetimeThai")
            val dateThai = datetimeThai.getString("dateThai")
            val timeThai = datetimeThai.getString("timeThai")
            val dateTimeString = "$dateThai $timeThai"
            
            // Extract hourly PM2.5 values and times
            val hourlyData = dataObject.getJSONArray("graphPredictByHrs")
            val hourlyReadings = mutableListOf<Pair<String, Double>>()
            for (i in 0 until hourlyData.length()) {
                val hourlyItem = hourlyData.getJSONArray(i)
                val pm25Value = hourlyItem.getDouble(0)
                val time = hourlyItem.getString(1)
                // Extract the time in 24-hour format (HH:mm)
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date.parse(time))
                hourlyReadings.add(Pair(formattedTime, pm25Value))
            }
            
            return PM25Data(pmCurrent, hourlyReadings, dateTimeString)
        }
    }
}
```

`android\app\src\main\java\com\example\test_wid_and\service\WidgetUpdateForegroundService.kt`
```kotlin
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
import com.example.test_wid_and.widget.MyHomeMediumWidget
import com.example.test_wid_and.widget.MyHomeSmallWidget
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import es.antonborri.home_widget.HomeWidgetPlugin
import kotlinx.coroutines.*

class WidgetUpdateForegroundService : Service() {
    companion object {
        private const val TAG = "WidgetUpdateFgService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "widget_update_channel"
        
        fun startService(context: Context) {
            val intent = Intent(context, WidgetUpdateForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "Service start requested")
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
                // Schedule next update with JobScheduler
                com.example.test_wid_and.util.JobSchedulerHelper.scheduleWidgetUpdateJob(this@WidgetUpdateForegroundService)
                // Stop the foreground service after update is done
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widgets", e)
                stopSelf()
            }
        }
        
        // If service is killed, restart it
        return START_STICKY
    }
    
    private suspend fun updateWidgets() {
        val currentTime = WidgetUtil.getCurrentTimeFormatted()
        Log.d(TAG, "Widget update started at $currentTime in foreground service")
        
        // Get location data from HomeWidgetPlugin
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
        
        if (latitude == null || longitude == null) {
            Log.e(TAG, "Failed to parse coordinates: $locationDataString")
            return
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

`android\app\src\main\java\com\example\test_wid_and\service\WidgetUpdateJobService.kt`
```kotlin
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
```

`android\app\src\main\java\com\example\test_wid_and\util\BatteryOptimizationHelper.kt`
```kotlin
package com.example.test_wid_and.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object BatteryOptimizationHelper {
    private const val TAG = "BatteryOptimHelper"
    
    // Check if app is ignoring battery optimizations
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // On older versions, battery optimizations were less aggressive
        }
    }
    
    // Create intent to request battery optimization exemption
    fun getBatteryOptimizationIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(context)) {
                Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:${context.packageName}")
                }
            } else {
                Log.d(TAG, "App is already ignoring battery optimizations")
                null
            }
        } else {
            Log.d(TAG, "Battery optimization settings not needed for Android < M")
            null
        }
    }
}
```

`android\app\src\main\java\com\example\test_wid_and\util\JobSchedulerHelper.kt`
```kotlin
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
        
        // Cancel any existing jobs
        jobScheduler.cancel(WidgetUpdateJobService.JOB_ID)
        
        // Use minimum periodic interval allowed by Android
        // For Android 8+ (Oreo), minimum is 15 minutes
        // For older Android versions, we can go as low as 1 minute
        val intervalMillis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TimeUnit.MINUTES.toMillis(15) // 15 mins for Android 8+
        } else {
            TimeUnit.MINUTES.toMillis(15) // 15 mins for older versions
        }
        
        val jobInfoBuilder = JobInfo.Builder(WidgetUpdateJobService.JOB_ID, componentName)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true) // Job persists across reboots
        
        // Set periodic timing based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // For Android 7+ (Nougat), we can use minimum flex interval
            jobInfoBuilder.setPeriodic(
                intervalMillis,
                JobInfo.getMinFlexMillis() // Minimum flex time
            )
        } else {
            jobInfoBuilder.setPeriodic(intervalMillis)
        }
        
        // Schedule the job
        val result = jobScheduler.schedule(jobInfoBuilder.build())
        
        if (result == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Widget update job scheduled successfully")
        } else {
            Log.e(TAG, "Failed to schedule widget update job")
        }
    }
    
    // Run immediate widget update using the foreground service
    fun runImmediateWidgetUpdate(context: Context) {
        // Start the foreground service for immediate update
        WidgetUpdateForegroundService.startService(context)
    }
}
```

`android\app\src\main\java\com\example\test_wid_and\util\WidgetUtil.kt`
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
        val cleanedDateThai = dateThai?.replace(Regex("(จันทร์|อังคาร|พุธ|พฤหัสบดี|ศุกร์|เสาร์|อาทิตย์)"), "")
        val dateWithDebugTime = "${cleanedDateThai ?: "No date"} [${currentTime}]"
        views.setTextViewText(R.id.date_text, dateWithDebugTime)
        
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
    
    // Get current Thai time in HH:mm:ss format
    fun getCurrentTimeFormatted(): String {
        val date = Date()  // Current time
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Asia/Bangkok")  // Set to Thai time
        return sdf.format(date)
    }
}
```


`android\app\src\main\java\com\example\test_wid_and\widget\MyHomeMediumWidget.kt`
```kotlin
package com.example.test_wid_and.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.test_wid_and.service.WidgetUpdateForegroundService
import com.example.test_wid_and.util.JobSchedulerHelper

/**
 * Implementation of App Widget functionality for medium widget.
 */
class MyHomeMediumWidget : AppWidgetProvider() {
    companion object {
        private const val TAG = "MyHomeMediumWidget"
    }

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
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            Log.d(TAG, "Received widget update request")
            WidgetUpdateForegroundService.startService(context)
        }
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "All widgets removed")
    }
}
```


`android\app\src\main\java\com\example\test_wid_and\widget\MyHomeSmallWidget.kt`
```kotlin
package com.example.test_wid_and.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.test_wid_and.service.WidgetUpdateForegroundService
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
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            Log.d(TAG, "Received widget update request")
            WidgetUpdateForegroundService.startService(context)
        }
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "All widgets removed")
    }
}
```


`android\app\src\main\java\com\example\test_wid_and\worker\WidgetUpdateWorker.kt`
```kotlin
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
                // Add backoff strategy for reliability
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
                
            // Use UPDATE policy as in the original code
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
```


`android\app\src\main\java\com\example\test_wid_and\WidgetApplication.kt`
```kotlin
package com.example.test_wid_and

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.example.test_wid_and.service.WidgetUpdateForegroundService
import com.example.test_wid_and.util.BatteryOptimizationHelper
import com.example.test_wid_and.util.JobSchedulerHelper

class WidgetApplication : Application(), Configuration.Provider, LifecycleObserver {
    companion object {
        private const val TAG = "WidgetApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created, setting up widget background updates")
        
        // Check battery optimization status
        val isBatteryOptimized = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)
        Log.d(TAG, "Battery optimization ignored: $isBatteryOptimized")
        
        // Initialize widget update mechanisms
        JobSchedulerHelper.scheduleWidgetUpdateJob(this)
        
        // Register as lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // Force immediate first update
        WidgetUpdateForegroundService.startService(this)
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Log.d(TAG, "App came to foreground - triggering immediate widget update")
        WidgetUpdateForegroundService.startService(this)
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
    }
}
```

`android\app\src\main\kotlin\com\example\test_wid_and\MainActivity.kt`
```kotlin
package com.example.test_wid_and

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
    private val CHANNEL = "com.example.test_wid_and/battery_optimization"
    
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

`android\app\src\main\res\xml\my_home_medium_widget_info.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Medium widget info -->
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/app_widget_description"
    android:initialKeyguardLayout="@layout/my_home_medium_widget"
    android:initialLayout="@layout/my_home_medium_widget"
    android:minWidth="40dp"
    android:minHeight="40dp"
    android:previewImage="@drawable/andwidjet1"
    android:previewLayout="@layout/my_home_medium_widget"
    android:resizeMode="horizontal|vertical"
    android:targetCellWidth="1"
    android:targetCellHeight="1"
    android:widgetCategory="home_screen" />
```

`android\app\src\main\res\xml\my_home_small_widget_info.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Small widget info -->
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="40dp"
    android:minHeight="40dp"
    android:previewImage="@drawable/andwidjet1"
    android:initialLayout="@layout/my_home_small_widget"
    android:description="@string/app_widget_description"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen"
    android:previewLayout="@layout/my_home_small_widget"
    android:targetCellHeight="1"
    android:targetCellWidth="1"
    android:initialKeyguardLayout="@layout/my_home_small_widget" />
```

`android\app\src\main\AndroidManifest.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" >

    <!--
     Required to query activities that can process text, see:
         https://developer.android.com/training/package-visibility and
         https://developer.android.com/reference/android/content/Intent#ACTION_PROCESS_TEXT.

         In particular, this is used by the Flutter engine in io.flutter.plugin.text.ProcessTextPlugin.
    -->
    <queries>
        <intent>
            <action android:name="android.intent.action.PROCESS_TEXT" />
            <data android:mimeType="text/plain" />
        </intent>
    </queries>

    <!-- Existing permissions -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    
    <!-- Additional permissions needed for reliable background widget updates -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

    <application
        android:name=".WidgetApplication"
        android:icon="@mipmap/ic_launcher"
        android:label="test_wid_and" >
        
        <!-- Widget update foreground service -->
        <service
            android:name=".service.WidgetUpdateForegroundService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="dataSync" />

        <!-- Widget update job service -->
        <service
            android:name=".service.WidgetUpdateJobService"
            android:permission="android.permission.BIND_JOB_SERVICE"
            android:exported="false" />

        <!-- System event receiver -->
        <receiver
            android:name=".receiver.SystemEventReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
                <action android:name="android.intent.action.TIME_CHANGED" />
                <action android:name="android.intent.action.TIMEZONE_CHANGED" />
            </intent-filter>
        </receiver>
        
        <!-- Updated widget receivers with new package paths and exported=true -->
        <receiver
            android:name=".widget.MyHomeSmallWidget"
            android:exported="true" >
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>

            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/my_home_small_widget_info" />
        </receiver>

        <service
            android:name=".LocationService"
            android:exported="false"
            android:foregroundServiceType="location" />

        <receiver
            android:name=".widget.MyHomeMediumWidget"
            android:exported="true" >
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>

            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/my_home_medium_widget_info" />
        </receiver>

        <activity
            android:name=".MainActivity"
            android:configChanges="orientation|keyboardHidden|keyboard|screenSize|smallestScreenSize|locale|layoutDirection|fontScale|screenLayout|density|uiMode"
            android:exported="true"
            android:hardwareAccelerated="true"
            android:launchMode="singleTop"
            android:taskAffinity=""
            android:theme="@style/LaunchTheme"
            android:windowSoftInputMode="adjustResize" >

            <!--
                 Specifies an Android theme to apply to this Activity as soon as
                 the Android process has started. This theme is visible to the user
                 while the Flutter UI initializes. After that, this theme continues
                 to determine the Window background behind the Flutter UI.
            -->
            <meta-data
                android:name="io.flutter.embedding.android.NormalTheme"
                android:resource="@style/NormalTheme" />

            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- WorkManager initialization provider -->
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup" />
        </provider>
        
        <!--
        Don't delete the meta-data below.
        This is used by the Flutter tool to generate GeneratedPluginRegistrant.java
        -->
        <meta-data
            android:name="flutterEmbedding"
            android:value="2" />
    </application>

</manifest>
```

`lib\constants\app_constants.dart`
```dart
class AppConstants {
  // App Group ID for widget communication
  static const String appGroupId = "group.homescreenaapp";
  
  // Widget names
  static const String iOSWidgetName = "MyHomeWidget";
  static const String androidMediumWidgetName = "MyHomeMediumWidget";
  static const String androidSmallWidgetName = "MyHomeSmallWidget";
  
  // Data keys
  static const String dataKey = "locationData_from_flutter_APP_new_5";
}
```


`lib\screens\location_page.dart`
```dart
import 'package:flutter/material.dart';
import '../services/location_service.dart';
import '../services/widget_service.dart';
import '../constants/app_constants.dart';
import 'package:home_widget/home_widget.dart';

class LocationPage extends StatefulWidget {
  @override
  _LocationPageState createState() => _LocationPageState();
}

class _LocationPageState extends State<LocationPage> with WidgetsBindingObserver {
  final LocationService _locationService = LocationService();
  final WidgetService _widgetService = WidgetService();
  String locationMessage = "Press the button to get location";

  @override
  void initState() {
    super.initState();
    // Register as an observer for app lifecycle changes
    WidgetsBinding.instance.addObserver(this);
    _initServices();
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
    if (state == AppLifecycleState.resumed) {
      // App has come to the foreground - update widgets
      print('App resumed - updating widgets');
      _getCurrentLocation();
    }
  }

  Future<void> _initServices() async {
    await _widgetService.initialize();
    _getCurrentLocation();
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
                await HomeWidget.updateWidget(
                  iOSName: AppConstants.iOSWidgetName,
                  androidName: AppConstants.androidMediumWidgetName
                );
                await HomeWidget.updateWidget(
                  androidName: AppConstants.androidSmallWidgetName
                );
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


`lib\services\location_service.dart`
```dart
import 'package:geolocator/geolocator.dart';

class LocationResult {
  final Position? position;
  final String? error;

  LocationResult({this.position, this.error});
}

class LocationService {
  Future<LocationResult> getCurrentLocation() async {
    // Check if location services are enabled
    bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      return LocationResult(error: "Location services are disabled.");
    }

    // Check location permission
    LocationPermission permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        return LocationResult(error: "Location permission denied.");
      }
    }

    if (permission == LocationPermission.deniedForever) {
      return LocationResult(
        error: "Location permissions are permanently denied."
      );
    }

    // Get current location
    try {
      Position position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );
      return LocationResult(position: position);
    } catch (e) {
      return LocationResult(error: "Error getting location: $e");
    }
  }
}
```


`lib\services\widget_service.dart`
```dart
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
      // Save location data for widgets
      await HomeWidget.saveWidgetData(AppConstants.dataKey, data);
      
      // Update iOS widget
      await HomeWidget.updateWidget(
        iOSName: AppConstants.iOSWidgetName
      );
      
      // Update Android widgets
      await HomeWidget.updateWidget(
        androidName: AppConstants.androidMediumWidgetName
      );
      await HomeWidget.updateWidget(
        androidName: AppConstants.androidSmallWidgetName
      );
      
      print("Widgets updated with location: $data at ${DateTime.now().toLocal()}");
    } catch (e) {
      print("Error updating widgets: $e");
    }
  }

  /// Force update widgets with last saved location
  Future<void> forceWidgetUpdate() async {
    try {
      // Update iOS widget
      await HomeWidget.updateWidget(
        iOSName: AppConstants.iOSWidgetName
      );
      
      // Update Android widgets
      await HomeWidget.updateWidget(
        androidName: AppConstants.androidMediumWidgetName
      );
      await HomeWidget.updateWidget(
        androidName: AppConstants.androidSmallWidgetName
      );
      
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


`lib\utils\permissions_helper.dart`
```dart
import 'package:geolocator/geolocator.dart';

class PermissionsHelper {
  static Future<bool> checkAndRequestLocationPermission() async {
    bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      return false;
    }

    LocationPermission permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        return false;
      }
    }

    if (permission == LocationPermission.deniedForever) {
      return false;
    }

    return true;
  }
}
```


`lib\main.dart`
```dart
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
    const platform = MethodChannel('com.example.test_wid_and/battery_optimization');
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