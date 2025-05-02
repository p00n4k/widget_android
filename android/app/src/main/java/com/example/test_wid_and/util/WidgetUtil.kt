package com.example.test_wid_and.util

import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import com.example.test_wid_and.R
import com.example.test_wid_and.util.LanguageHelper
import java.text.SimpleDateFormat
import java.util.*

object WidgetUtil {
    
    data class PM25Data(
        val pmCurrent: Double?,
        val hourlyReadings: List<Pair<String, Double>>?,
        val dateThai: String?
    )
    
    fun buildWidgetViews(context: Context, layoutId: Int, pm25Data: PM25Data): RemoteViews {
        // Get language-configured context
        val configuredContext = LanguageHelper.getConfiguredContext(context)
        
        val views = RemoteViews(context.packageName, layoutId)
        
        // Extract data from PM25Data object
        val pmCurrent = pm25Data.pmCurrent
        val hourlyData = pm25Data.hourlyReadings
        val dateText = pm25Data.dateThai
        
        // Set PM2.5 value
        views.setTextViewText(R.id.text_pm25, pmCurrent?.let { String.format("%.0f", it) } 
            ?: LanguageHelper.getStringResource(context, R.string.no_data))
        
        // Determine background, images, and text color based on PM2.5 value
        val (backgroundResId, humanImage, textColor, message) = when {
            pmCurrent == null -> {
                Quadruple(R.drawable.andwidjet1, R.drawable.verygood, "#FFFFFF", 
                    LanguageHelper.getStringResource(context, R.string.no_data))
            }
            pmCurrent <= 15 -> {
                Quadruple(R.drawable.andwidjet1, R.drawable.verygood, "#FFFFFF", 
                    LanguageHelper.getStringResource(context, R.string.air_very_good))
            }
            pmCurrent <= 25 -> {
                views.setImageViewResource(R.id.nearme_id, R.drawable.near_me_dark)
                Quadruple(R.drawable.andwidjet2, R.drawable.good, "#303C46", 
                    LanguageHelper.getStringResource(context, R.string.air_good))
            }
            pmCurrent <= 37.5 -> {
                views.setImageViewResource(R.id.nearme_id, R.drawable.near_me_dark)
                Quadruple(R.drawable.andwidjet3, R.drawable.medium, "#303C46", 
                    LanguageHelper.getStringResource(context, R.string.air_moderate))
            }
            pmCurrent <= 75 -> {
                Quadruple(R.drawable.andwidjet4, R.drawable.bad, "#FFFFFF", 
                    LanguageHelper.getStringResource(context, R.string.air_unhealthy))
            }
            else -> {
                Quadruple(R.drawable.andwidjet5, R.drawable.verybad, "#FFFFFF", 
                    LanguageHelper.getStringResource(context, R.string.air_very_unhealthy))
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
        views.setTextViewText(R.id.text_pm25_header, LanguageHelper.getStringResource(context, R.string.pm25_hourly))
        
        // Set PM2.5 unit text
        views.setTextViewText(R.id.text_pm25_unit, LanguageHelper.getStringResource(context, R.string.pm25_unit))
        
        // Add language debug info if needed
        val currentLang = LanguageHelper.getCurrentLanguageCode(context)
        val langDisplay = LanguageHelper.getStringResource(context, R.string.current_language)
        val langWithCode = String.format(langDisplay, if (currentLang == "en") "Eng" else "ไทย")
        
        // Clean and set date text
        views.setTextViewText(R.id.date_text, dateText ?: LanguageHelper.getStringResource(context, R.string.no_data))
        
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
                setTextViewText(R.id.hour_text, LanguageHelper.getStringResource(context, R.string.no_data))
                setTextViewText(R.id.pm_text, LanguageHelper.getStringResource(context, R.string.no_data))
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