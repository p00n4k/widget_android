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
      
      // Set in main.dart global variable
      // This is done outside this function in the calling code
      
      // Send to widgets via HomeWidget
      await HomeWidget.saveWidgetData(AppConstants.appLanguageData, 
        languageCode == 'Eng' ? 'en' : 'th');
      
      // If on Android, send via method channel too
      if (Platform.isAndroid) {
        await _channel.invokeMethod('changeLanguage', {'language': languageCode});
      }
      
      print('Language updated to: $languageCode');
      
      // No need to force update widgets here as caller will do it
    } catch (e) {
      print('Error updating language: $e');
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