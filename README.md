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