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






There are 2 bugs in this flutter code.
1. when I tab on button `Get Location` it suppose to get a new location and update an app, but when I click on it, it still show the same lat lon
2. when I click on `Force Update Widget`, it should update a widget instantly, but it is not working, and I also got the error below

Analyze the code and fix these 2 bug

```
I/flutter ( 5701): Force updating widgets
I/flutter ( 5701): Error force updating widgets: PlatformException(-3, No Widget found with Name null. Argument 'name' must be the same as your AppWidgetProvider you wish to update, java.lang.ClassNotFoundException: com.example.test_wid_and.null
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
  
  // Language channel
  static const String languageChannel = "com.check_phoon_widget/language";
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
  String lang = "Eng"; // Default language
  
  // Track if we've updated widgets in this session
  bool _hasUpdatedWidgetsThisSession = false;

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
    setState(() {
      lang = _widgetService.currentLanguage;
    });
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
          print("Latitude: ${locationResult.position!.latitude}, Longitude: ${locationResult.position!.longitude}");
          locationMessage = "Latitude: ${locationResult.position!.latitude}, "
              "Longitude: ${locationResult.position!.longitude}";
          
          // Update widgets with new location and language
          _widgetService.updateWidgetsWithLocation(
            locationResult.position!.latitude, 
            locationResult.position!.longitude,
            language: lang
          );
        }
      });
    } catch (e) {
      setState(() {
        locationMessage = "Error getting location: $e";
      });
    }
  }

  Future<void> _changeLanguage(String newLang) async {
    if (newLang != lang) {
      await _widgetService.changeLanguage(newLang);
      setState(() {
        lang = newLang;
      });
      // Force widget update with new language
      _getCurrentLocation();
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
            // Language selection
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text("Language: "),
                DropdownButton<String>(
                  value: lang,
                  items: [
                    DropdownMenuItem(value: "Eng", child: Text("English")),
                    DropdownMenuItem(value: "ไทย", child: Text("Thai")),
                  ],
                  onChanged: (value) {
                    if (value != null) {
                      _changeLanguage(value);
                    }
                  },
                ),
              ],
            ),
            // Display current language for debugging
            Text("Current Language: $lang", 
                style: TextStyle(fontSize: 12, color: Colors.grey)),
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


`lib\services\location_service.dart`
```dart
// lib/services/location_service.dart
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
// lib/services/widget_service.dart
import 'package:flutter/services.dart';
import 'package:home_widget/home_widget.dart';
import '../constants/app_constants.dart';
import 'package:geolocator/geolocator.dart';

class WidgetService {
  final MethodChannel _languageChannel = MethodChannel(AppConstants.languageChannel);
  String _currentLanguage = "Eng"; // Default language
  
  Future<void> initialize() async {
    await HomeWidget.setAppGroupId(AppConstants.appGroupId);
    // Get saved language from native side
    try {
      _currentLanguage = await _languageChannel.invokeMethod('getCurrentLanguage') ?? "Eng";
      print("Current language from native: $_currentLanguage");
    } catch (e) {
      print("Error getting current language: $e");
    }
  }

  // Getter for the current language
  String get currentLanguage => _currentLanguage;

  // Method to change language
  Future<void> changeLanguage(String language) async {
    if (language != "Eng" && language != "ไทย") {
      return; // Invalid language
    }
    
    try {
      await _languageChannel.invokeMethod('changeLanguage', {'language': language});
      _currentLanguage = language;
      print("Language changed to: $_currentLanguage");
      
      // Force update widgets with new language
      await updateWidgetsWithLocation(null, null, language: language);
    } catch (e) {
      print("Error changing language: $e");
    }
  }

  Future<void> updateWidgetsWithLocation(double? latitude, double? longitude, {String? language}) async {
    // Use provided language or current language
    final lang = language ?? _currentLanguage;
    print("Updating widgets with language: $lang");
    
    // If lat/long are not provided, try to get from saved data
    double? lat = latitude;
    double? long = longitude;
    
    if (lat == null || long == null) {
      final savedData = await HomeWidget.getWidgetData<String>(AppConstants.dataKey);
      if (savedData != null) {
        final parts = savedData.split(',');
        if (parts.length >= 2) {
          lat = double.tryParse(parts[0]);
          long = double.tryParse(parts[1]);
        }
      }
      
      // If still null, get last known position
      if (lat == null || long == null) {
        try {
          final lastPosition = await Geolocator.getLastKnownPosition();
          if (lastPosition != null) {
            lat = lastPosition.latitude;
            long = lastPosition.longitude;
          }
        } catch (e) {
          print("Error getting last position: $e");
        }
      }
    }
    
    // If we have coordinates, update widgets
    if (lat != null && long != null) {
      String data = "$lat,$long,$lang";
      
      try {
        // Save location data for widgets
        await HomeWidget.saveWidgetData(AppConstants.dataKey, data);
        
        // Update iOS widget - specify null for androidName
        await HomeWidget.updateWidget(
          iOSName: AppConstants.iOSWidgetName,
          androidName: null
        );
        
        // Update Android widgets - specify each one separately
        await HomeWidget.updateWidget(
          androidName: AppConstants.androidMediumWidgetName
        );
        
        await HomeWidget.updateWidget(
          androidName: AppConstants.androidSmallWidgetName
        );
        
        print("Widgets updated with: $data at ${DateTime.now().toLocal()}");
      } catch (e) {
        print("Error updating widgets: $e");
      }
    } else {
      print("No coordinates available for widget update");
    }
  }

  /// Force update widgets with last saved location
  Future<void> forceWidgetUpdate() async {
    try {
      print("Force updating widgets");
      
      // Update iOS widget - specify null for androidName
      await HomeWidget.updateWidget(
        iOSName: AppConstants.iOSWidgetName,
        androidName: null
      );
      
      // Update Android widgets - specify each one separately
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
// lib/utils/permissions_helper.dart
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
// lib/main.dart
import 'package:flutter/material.dart';
import 'package:home_widget/home_widget.dart';
import 'screens/location_page.dart';
import 'constants/app_constants.dart';
import 'services/widget_service.dart';
import 'dart:io';
import 'package:flutter/services.dart';

// Global variable for language preference
String lang = "Eng"; // Default to English

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
  
  // Get the current language setting
  lang = widgetService.currentLanguage;
  print('Current language from widget service: $lang');
  
  // iOS widget
  if (Platform.isIOS) {
    await HomeWidget.updateWidget(
      iOSName: AppConstants.iOSWidgetName,
    );
  }
  
  // Android widgets - update each one separately
  if (Platform.isAndroid) {
    await HomeWidget.updateWidget(
      androidName: AppConstants.androidMediumWidgetName
    );
    
    await HomeWidget.updateWidget(
      androidName: AppConstants.androidSmallWidgetName
    );
  }
  
  print('Widgets updated on app start with language: $lang');
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








