package com.check_phoon.android_widget.util

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.widget.RemoteViews
import com.check_phoon.android_widget.R
import com.check_phoon.android_widget.service.PM25Service
import java.text.SimpleDateFormat
import java.util.*

object WidgetUtil {
    
    private const val TAG = "WidgetUtil"
    
    fun buildWidgetViews(
        context: Context, 
        layoutId: Int, 
        pm25Data: PM25Service.Companion.PM25Data,
        languageCode: String
    ): RemoteViews {
        val views = RemoteViews(context.packageName, layoutId)
        
        try {
            // Extract data from PM25Data object safely
            val pmCurrent = pm25Data.pmCurrent
            val hourlyData = pm25Data.hourlyReadings
            val dateString = pm25Data.dateString
            val timeString = pm25Data.timeString
            
            // Display PM2.5 value safely
            val pm25Text = when {
                pmCurrent == null -> context.getString(R.string.no_data)
                pmCurrent.isNaN() || pmCurrent.isInfinite() -> context.getString(R.string.no_data)
                else -> String.format("%.0f", pmCurrent)
            }
            views.setTextViewText(R.id.text_pm25, pm25Text)
            
            // Determine background, images, and text color based on PM2.5 value
            val (backgroundResId, humanImage, textColor, messageResId) = when {
                pmCurrent == null || pmCurrent.isNaN() || pmCurrent.isInfinite() -> {
                    Quadruple(R.drawable.andwidjet1, R.drawable.verygood, "#FFFFFF", R.string.no_data)
                }
                pmCurrent <= 15 -> {
                    Quadruple(R.drawable.andwidjet1, R.drawable.verygood, "#FFFFFF", R.string.air_very_good)
                }
                pmCurrent <= 25 -> {
                    views.setImageViewResource(R.id.nearme_id, R.drawable.near_me_dark)
                    Quadruple(R.drawable.andwidjet2, R.drawable.good, "#303C46", R.string.air_good)
                }
                pmCurrent <= 37.5 -> {
                    views.setImageViewResource(R.id.nearme_id, R.drawable.near_me_dark)
                    Quadruple(R.drawable.andwidjet3, R.drawable.medium, "#303C46", R.string.air_moderate)
                }
                pmCurrent <= 75 -> {
                    Quadruple(R.drawable.andwidjet4, R.drawable.bad, "#FFFFFF", R.string.air_unhealthy)
                }
                else -> {
                    Quadruple(R.drawable.andwidjet5, R.drawable.verybad, "#FFFFFF", R.string.air_very_unhealthy)
                }
            }
            
            // Apply widget UI configuration
            views.setImageViewResource(R.id.widget_background, backgroundResId)
            views.setImageViewResource(R.id.human_image, humanImage)
            views.setTextViewText(R.id.text_recomend, context.getString(messageResId))
            
            // Set text colors
            val colorParsed = Color.parseColor(textColor)
            views.setTextColor(R.id.text_recomend, colorParsed)
            views.setTextColor(R.id.text_pm25, colorParsed)
            views.setTextColor(R.id.text_pm25_unit, colorParsed)
            views.setTextColor(R.id.text_pm25_header, colorParsed)
            views.setTextColor(R.id.date_text, colorParsed)
            views.setTextColor(R.id.time_text, colorParsed)
            
            // Set hourly header text from string resources
            views.setTextViewText(R.id.text_pm25_header, context.getString(R.string.pm25_hourly))
            
            // Set units text from resources
            views.setTextViewText(R.id.text_pm25_unit, context.getString(R.string.pm25_unit))
            
            // Set date and time text safely
            val displayDate = dateString ?: context.getString(R.string.no_data)
            views.setTextViewText(R.id.date_text, displayDate)
            
            val displayTime = timeString ?: context.getString(R.string.no_data)
            views.setTextViewText(R.id.time_text, displayTime)
            
            // Set text size and max lines for date and time
            views.setTextViewTextSize(R.id.date_text, TypedValue.COMPLEX_UNIT_SP, 12f)
            views.setInt(R.id.date_text, "setMaxLines", 1)
            views.setInt(R.id.date_text, "setGravity", android.view.Gravity.CENTER)
            
            views.setTextViewTextSize(R.id.time_text, TypedValue.COMPLEX_UNIT_SP, 12f)
            views.setInt(R.id.time_text, "setMaxLines", 1)
            views.setInt(R.id.time_text, "setGravity", android.view.Gravity.CENTER)
            
            // Process hourly data for the three fixed boxes
            if (!hourlyData.isNullOrEmpty()) {
                // We have 3 fixed hour boxes, so we'll fill them with available data
                val hourBoxIds = listOf(
                    Pair(R.id.hour_text_1, R.id.pm_text_1),
                    Pair(R.id.hour_text_2, R.id.pm_text_2),
                    Pair(R.id.hour_text_3, R.id.pm_text_3)
                )
                
                // Fill available data, limited to 3 entries
                val limitedData = hourlyData.take(3)
                for (i in limitedData.indices) {
                    if (i < hourBoxIds.size) {
                        val (hour, pm25Value) = limitedData[i]
                        val (hourTextId, pmTextId) = hourBoxIds[i]
                        
                        // Skip invalid values
                        if (pm25Value.isNaN() || pm25Value.isInfinite()) {
                            views.setTextViewText(hourTextId, hour)
                            views.setTextViewText(pmTextId, "-")
                        } else {
                            views.setTextViewText(hourTextId, hour)
                            views.setTextViewText(pmTextId, String.format("%.0f", pm25Value))
                        }
                        
                        // Set text color for these elements
                        views.setTextColor(hourTextId, colorParsed)
                        views.setTextColor(pmTextId, colorParsed)
                    }
                }
                
                // For any remaining boxes (if less than 3 entries), set to "--"
                for (i in limitedData.size until hourBoxIds.size) {
                    val (hourTextId, pmTextId) = hourBoxIds[i]
                    views.setTextViewText(hourTextId, "--")
                    views.setTextViewText(pmTextId, "--")
                    views.setTextColor(hourTextId, colorParsed)
                    views.setTextColor(pmTextId, colorParsed)
                }
            } else {
                // No data available, set all to "--"
                val hourBoxIds = listOf(
                    Pair(R.id.hour_text_1, R.id.pm_text_1),
                    Pair(R.id.hour_text_2, R.id.pm_text_2),
                    Pair(R.id.hour_text_3, R.id.pm_text_3)
                )
                
                for ((hourTextId, pmTextId) in hourBoxIds) {
                    views.setTextViewText(hourTextId, "--")
                    views.setTextViewText(pmTextId, "--")
                    views.setTextColor(hourTextId, colorParsed)
                    views.setTextColor(pmTextId, colorParsed)
                }
            }
            
        } catch (e: Exception) {
            // If anything fails, set to fallback state
            Log.e(TAG, "Error building widget views", e)
            
            // Set fallback values
            views.setTextViewText(R.id.text_pm25, context.getString(R.string.no_data))
            views.setImageViewResource(R.id.widget_background, R.drawable.andwidjet1)
            views.setImageViewResource(R.id.human_image, R.drawable.verygood)
            views.setTextViewText(R.id.text_recomend, context.getString(R.string.no_data))
            
            // Set default text color
            val colorParsed = Color.parseColor("#FFFFFF")
            views.setTextColor(R.id.text_recomend, colorParsed)
            views.setTextColor(R.id.text_pm25, colorParsed)
            views.setTextColor(R.id.text_pm25_unit, colorParsed)
            views.setTextColor(R.id.text_pm25_header, colorParsed)
            views.setTextColor(R.id.date_text, colorParsed)
            views.setTextColor(R.id.time_text, colorParsed)
            
            // Set default headers
            views.setTextViewText(R.id.text_pm25_header, context.getString(R.string.pm25_hourly))
            views.setTextViewText(R.id.text_pm25_unit, context.getString(R.string.pm25_unit))
            views.setTextViewText(R.id.date_text, context.getString(R.string.no_data))
            views.setTextViewText(R.id.time_text, context.getString(R.string.no_data))
            
            // Set default values for hour boxes
            val hourBoxIds = listOf(
                Pair(R.id.hour_text_1, R.id.pm_text_1),
                Pair(R.id.hour_text_2, R.id.pm_text_2),
                Pair(R.id.hour_text_3, R.id.pm_text_3)
            )
            
            for ((hourTextId, pmTextId) in hourBoxIds) {
                views.setTextViewText(hourTextId, "--")
                views.setTextViewText(pmTextId, "--")
                views.setTextColor(hourTextId, colorParsed)
                views.setTextColor(pmTextId, colorParsed)
            }
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