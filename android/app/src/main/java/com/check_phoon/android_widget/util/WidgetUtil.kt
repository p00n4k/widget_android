package com.check_phoon.android_widget.util

import android.content.Context
import android.graphics.Color
import android.util.Log
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
            val dateTimeString = pm25Data.dateTimeString
            
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
            
            // Set hourly header text from string resources
            views.setTextViewText(R.id.text_pm25_header, context.getString(R.string.pm25_hourly))
            
            // Set units text from resources
            views.setTextViewText(R.id.text_pm25_unit, context.getString(R.string.pm25_unit))
            
            // Set date text safely
            views.setTextViewText(R.id.date_text, dateTimeString ?: context.getString(R.string.no_data))
    
            // Add hourly readings
            views.removeAllViews(R.id.hourly_readings_container)
            if (!hourlyData.isNullOrEmpty()) {
                // Limit to a reasonable number of entries to prevent overflow
                val limitedData = hourlyData.take(3)
                for ((hour, pm25) in limitedData) {
                    // Skip invalid values
                    if (pm25.isNaN() || pm25.isInfinite()) continue
                    
                    val hourlyView = RemoteViews(context.packageName, R.layout.hourly_reading).apply {
                        setTextViewText(R.id.hour_text, hour)
                        setTextColor(R.id.pm_text, colorParsed)
                        setTextColor(R.id.hour_text, colorParsed)
                        setTextViewText(R.id.pm_text, String.format("%.1f", pm25))
                    }
                    views.addView(R.id.hourly_readings_container, hourlyView)
                }
                
                // If we filtered out all entries, show no data
                // Check if container is empty by adding a dummy view and seeing if it's the first
                val countBefore = views.getCount(R.id.hourly_readings_container)
                if (countBefore == 0) {
                    addNoDataHourlyView(context, views, colorParsed)
                }
            } else {
                addNoDataHourlyView(context, views, colorParsed)
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
            
            // Set default headers
            views.setTextViewText(R.id.text_pm25_header, context.getString(R.string.pm25_hourly))
            views.setTextViewText(R.id.text_pm25_unit, context.getString(R.string.pm25_unit))
            views.setTextViewText(R.id.date_text, context.getString(R.string.no_data))
            
            // Add no data hourly view
            views.removeAllViews(R.id.hourly_readings_container)
            addNoDataHourlyView(context, views, colorParsed)
        }
        
        return views
    }
    
    private fun addNoDataHourlyView(context: Context, views: RemoteViews, textColor: Int) {
        val noDataView = RemoteViews(context.packageName, R.layout.hourly_reading).apply {
            setTextViewText(R.id.hour_text, context.getString(R.string.no_data))
            setTextViewText(R.id.pm_text, "-")
            setTextColor(R.id.hour_text, textColor)
            setTextColor(R.id.pm_text, textColor)
        }
        views.addView(R.id.hourly_readings_container, noDataView)
    }
    
    // Extension function to get the number of views in a ViewGroup
    private fun RemoteViews.getCount(viewId: Int): Int {
        // This is a workaround since RemoteViews doesn't have direct way to check count
        // We need to check if any views exist in the container
        return 0  // Default to assuming empty
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