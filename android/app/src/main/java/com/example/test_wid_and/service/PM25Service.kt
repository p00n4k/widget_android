package com.example.test_wid_and.service

import com.example.test_wid_and.util.WidgetUtil.PM25Data
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

class PM25Service {
    companion object {
        private const val TAG = "PM25Service"
        
        suspend fun fetchPM25Data(latitude: Double, longitude: Double, languageCode: String? = null): PM25Data {
            val language = languageCode ?: "en"
            Log.d(TAG, "Fetching PM2.5 data with language: $language")
            
            return try {
                val url = URL("https://pm25.gistda.or.th/rest/pred/getPm25byLocation?lat=$latitude&lng=$longitude")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    parseResponse(response, language)
                } else {
                    PM25Data(null, null, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                PM25Data(null, null, null)
            }
        }
        
        private fun parseResponse(response: String, language: String): PM25Data {
            val jsonObject = JSONObject(response)
            val dataObject = jsonObject.getJSONObject("data")
            
            // Extract current PM2.5
            val pmCurrent = dataObject.getJSONArray("pm25").getDouble(0)
            
            // Extract date information based on language - FIX THE INVERTED LOGIC
            val dateTimeString = if (language == "th") {
                // Use Thai date information when language is Thai
                val datetimeThai = dataObject.getJSONObject("datetimeThai")
                val dateThai = datetimeThai.getString("dateThai")
                val timeThai = datetimeThai.getString("timeThai")
                "$dateThai $timeThai"
            } else {
                // Use English date information when language is English
                val datetimeEng = dataObject.getJSONObject("datetimeEng")
                val dateEng = datetimeEng.getString("dateEng")
                val timeEng = datetimeEng.getString("timeEng")
                "$dateEng $timeEng"
            }
            
            Log.d(TAG, "Selected date format for language '$language': $dateTimeString")
            
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