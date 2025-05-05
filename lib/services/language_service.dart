import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../constants/app_constants.dart';
import 'package:home_widget/home_widget.dart';
import 'dart:io' show Platform;

class LanguageService {
  final MethodChannel _channel = MethodChannel(AppConstants.languageChannel);
  
  Future<void> initialize() async {
    // Initialize method channel
  }
  
  // Update language both in app and in widgets
  Future<void> updateLanguage(String languageCode) async {
    try {
      // Save to SharedPreferences
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('language', languageCode);
      
      // Send to widgets via HomeWidget
      final nativeLanguageCode = languageCode == 'Eng' ? 'en' : 'th';
      await HomeWidget.saveWidgetData(AppConstants.appLanguageData, nativeLanguageCode);
      
      // If on Android, send via method channel too
      if (Platform.isAndroid) {
        await _channel.invokeMethod('changeLanguage', {'language': languageCode});
      } else if (Platform.isIOS) {
        // For iOS, explicitly request update
        await HomeWidget.updateWidget(iOSName: AppConstants.iOSWidgetName);
      }
      
      print('Language updated to: $languageCode');
      
      // Force update the widgets to refresh UI
      await _forceWidgetUpdate();
    } catch (e) {
      print('Error updating language: $e');
    }
  }
  
  // Force update all widgets
  Future<void> _forceWidgetUpdate() async {
    try {
      if (Platform.isIOS) {
        await HomeWidget.updateWidget(iOSName: AppConstants.iOSWidgetName);
      } else if (Platform.isAndroid) {
        await HomeWidget.updateWidget(androidName: AppConstants.androidMediumWidgetName);
        await HomeWidget.updateWidget(androidName: AppConstants.androidSmallWidgetName);
      }
    } catch (e) {
      print('Error forcing widget update: $e');
    }
  }
  
  // Get the current language
  Future<String> getCurrentLanguage() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      return prefs.getString('language') ?? 'Eng';
    } catch (e) {
      print('Error getting current language: $e');
      return 'Eng';
    }
  }
}