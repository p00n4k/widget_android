// android/app/src/main/java/com/example/android_widget/service/PM25Service.kt
package com.check_phoon.android_widget.service

import com.check_phoon.android_widget.util.WidgetUtil.PM25Data
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