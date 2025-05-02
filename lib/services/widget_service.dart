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