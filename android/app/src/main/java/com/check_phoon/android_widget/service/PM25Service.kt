package com.check_phoon.android_widget.service

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class PM25Service {
    companion object {
        private const val TAG = "PM25Service"
        
        // Data classes for PM25 data
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

        private data class PM25Cache(
            val rawResponse: String,
            val timestamp: Long,
            val parsedDataEn: PM25Data,
            val parsedDataTh: PM25Data
        )
        
        // Cache for the latest response
        private var cache: PM25Cache? = null
        private const val CACHE_VALID_TIME = 15 * 60 * 1000 // 15 minutes
        private const val MAX_RETRIES = 5
        private const val RETRY_DELAY_MS = 3000L // 3 seconds
        
        /**
         * Fetches PM2.5 data from API with retry mechanism
         */
        suspend fun fetchPM25Data(latitude: Double, longitude: Double, languageCode: String = "en"): PM25Data {
            Log.d(TAG, "Fetching PM2.5 data with language: $languageCode")
            
            // Try getting cached data first
            getCachedData(languageCode)?.let {
                Log.d(TAG, "Using cached data")
                return it
            }
            
            return withContext(Dispatchers.IO) {
                var lastException: Exception? = null
                
                // Retry logic with delay
                for (attempt in 1..MAX_RETRIES) {
                    try {
                        val url = URL("https://pm25.gistda.or.th/rest/pred/getPm25byLocation?lat=$latitude&lng=$longitude")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000
                        connection.connect()
                        
                        if (connection.responseCode == 200) {
                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            Log.d(TAG, "API response received (first 100 chars): ${response.take(100)}...")
                            
                            try {
                                // Parse data for both languages
                                val dataEn = parseResponse(response, "en", latitude, longitude)
                                val dataTh = parseResponse(response, "th", latitude, longitude)
                                
                                // Cache the raw response and parsed data for both languages
                                cache = PM25Cache(
                                    rawResponse = response,
                                    timestamp = System.currentTimeMillis(),
                                    parsedDataEn = dataEn,
                                    parsedDataTh = dataTh
                                )
                                
                                // Return data for requested language
                                return@withContext if (languageCode == "th") dataTh else dataEn
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing response: ${e.message}", e)
                                lastException = e
                            }
                        } else {
                            Log.w(TAG, "HTTP error: ${connection.responseCode}")
                        }
                    } catch (e: Exception) {
                        lastException = e
                        Log.w(TAG, "Fetch attempt $attempt failed: ${e.message}")
                        
                        if (attempt < MAX_RETRIES) {
                            delay(RETRY_DELAY_MS)
                            Log.d(TAG, "Retrying in ${RETRY_DELAY_MS}ms...")
                        }
                    }
                }
                
                // All retries failed
                lastException?.let {
                    Log.e(TAG, "All retries failed", it)
                }
                
                // Return empty data as fallback
                PM25Data(null, null, null, null)
            }
        }
        
        /**
         * Clears the cache
         */
        fun clearCache() {
            cache = null
            Log.d(TAG, "Cache cleared")
        }
        
        /**
         * Gets cached PM2.5 data for the specified language
         * Returns null if cache is invalid or doesn't exist
         */
        fun getCachedData(languageCode: String = "en"): PM25Data? {
            Log.d(TAG, "Getting cached data for language: $languageCode")
            
            val currentCache = cache
            if (currentCache == null) {
                Log.d(TAG, "No cache available")
                return null
            }
            
            // Check if cache is still valid
            if (System.currentTimeMillis() - currentCache.timestamp < CACHE_VALID_TIME) {
                return if (languageCode == "th") currentCache.parsedDataTh else currentCache.parsedDataEn
            } else {
                Log.d(TAG, "Cache expired")
                return null
            }
        }
        
        private fun parseResponse(response: String, language: String, latitude: Double, longitude: Double): PM25Data {
            try {
                val jsonObject = JSONObject(response)
                
                // Check if data object exists
                if (!jsonObject.has("data")) {
                    Log.e(TAG, "Response missing 'data' object")
                    return PM25Data(null, null, null, null)
                }
                
                val dataObject = jsonObject.getJSONObject("data")
                
                // Extract current PM2.5 with safety checks
                var pmCurrent: Double? = null
                try {
                    if (dataObject.has("pm25") && !dataObject.isNull("pm25")) {
                        val pm25Array = dataObject.getJSONArray("pm25")
                        if (pm25Array.length() > 0) {
                            pmCurrent = pm25Array.optDouble(0)
                            // Check for NaN or Infinity
                            if (pmCurrent.isNaN() || pmCurrent.isInfinite()) {
                                pmCurrent = null
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting current PM2.5: ${e.message}")
                }
                
                // Extract date information based on language with safety checks
                var dateTimeString: String? = null
                try {
                    if (language == "th" && dataObject.has("datetimeThai")) {
                        val datetimeThai = dataObject.getJSONObject("datetimeThai")
                        val dateThai = datetimeThai.optString("dateThai", "")
                        val timeThai = datetimeThai.optString("timeThai", "")
                        if (dateThai.isNotEmpty() || timeThai.isNotEmpty()) {
                            dateTimeString = "$dateThai $timeThai"
                        }
                    } else if (dataObject.has("datetimeEng")) {
                        val datetimeEng = dataObject.getJSONObject("datetimeEng")
                        val dateEng = datetimeEng.optString("dateEng", "")
                        val timeEng = datetimeEng.optString("timeEng", "")
                        if (dateEng.isNotEmpty() || timeEng.isNotEmpty()) {
                            dateTimeString = "$dateEng $timeEng"
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting date information: ${e.message}")
                }
                
                // Extract location information
                val locationData = try {
                    extractLocationData(dataObject, language, latitude, longitude)
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting location data: ${e.message}")
                    null
                }
                
                // Extract hourly readings with safety checks
                val hourlyReadings = mutableListOf<Pair<String, Double>>()
                try {
                    if (dataObject.has("graphPredictByHrs") && !dataObject.isNull("graphPredictByHrs")) {
                        val hourlyData = dataObject.getJSONArray("graphPredictByHrs")
                        
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        
                        for (i in 0 until hourlyData.length()) {
                            try {
                                val hourlyItem = hourlyData.getJSONArray(i)
                                if (hourlyItem.length() >= 2) {
                                    val pm25Value = hourlyItem.optDouble(0)
                                    // Skip NaN or Infinity values
                                    if (pm25Value.isNaN() || pm25Value.isInfinite()) {
                                        continue
                                    }
                                    
                                    val timeStr = hourlyItem.optString(1, "")
                                    if (timeStr.isNotEmpty()) {
                                        try {
                                            val parsedDate = inputFormat.parse(timeStr)
                                            if (parsedDate != null) {
                                                val formattedTime = outputFormat.format(parsedDate)
                                                hourlyReadings.add(Pair(formattedTime, pm25Value))
                                            }
                                        } catch (e: Exception) {
                                            Log.w(TAG, "Error parsing time: $timeStr - ${e.message}")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error processing hourly item at index $i: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting hourly readings: ${e.message}")
                }
                
                // If we have no PM2.5 data but have hourly readings, use the first hourly reading as current
                if (pmCurrent == null && hourlyReadings.isNotEmpty()) {
                    pmCurrent = hourlyReadings.first().second
                    Log.d(TAG, "Using first hourly reading as current PM2.5: $pmCurrent")
                }
                
                val result = PM25Data(pmCurrent, hourlyReadings, dateTimeString, locationData)
                Log.d(TAG, "Parsed data: PM2.5=${result.pmCurrent}, hourly count=${result.hourlyReadings?.size ?: 0}")
                return result
                
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing response: ${e.message}")
                e.printStackTrace()
                return PM25Data(null, null, null, null)
            }
        }
        
        /**
        * Extracts location data based on the specified language
        */
        private fun extractLocationData(dataObject: JSONObject, language: String, latitude: Double, longitude: Double): LocationData? {
            if (!dataObject.has("loc") || dataObject.isNull("loc")) {
                return null
            }
            
            val locObject = dataObject.getJSONObject("loc")
            
            // Extract tambon ID with default value
            val tbIdn = locObject.optInt("tb_idn", 0)
            
            // Extract location names based on language, using empty string as fallback
            val tambon: String
            val amphoe: String
            val province: String
            
            if (language == "th") {
                // Thai version
                tambon = locObject.optString("tb_tn", "")
                amphoe = locObject.optString("ap_tn", "")
                province = locObject.optString("pv_tn", "")
            } else {
                // English version
                tambon = locObject.optString("tb_en", "")
                amphoe = locObject.optString("ap_en", "")
                province = locObject.optString("pv_en", "")
            }
            
            return LocationData(latitude, longitude, tbIdn, tambon, amphoe, province)
        }
    }
}