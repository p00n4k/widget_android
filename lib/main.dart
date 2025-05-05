// lib/main.dart
import 'package:flutter/material.dart';
import 'package:home_widget/home_widget.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'screens/location_page.dart';
import 'constants/app_constants.dart';
import 'services/widget_service.dart';
import 'services/language_service.dart';
import 'dart:io';
import 'package:flutter/services.dart';

// Global language variable
String lang = 'Eng';  // Default language

void main() async {  // Changed to async
  WidgetsFlutterBinding.ensureInitialized();
  
  // Initialize language from saved preferences
  final prefs = await SharedPreferences.getInstance();
  lang = prefs.getString('language') ?? 'Eng';
  
  // Set up HomeWidget
  HomeWidget.setAppGroupId(AppConstants.appGroupId);
  
  // Initialize LanguageService
  final languageService = LanguageService();
  await languageService.initialize();
  
  // Request battery optimization exception on Android
  if (Platform.isAndroid) {
    _requestBatteryOptimizationExemption();
    
    // Send language to Android
    await languageService.updateLanguage(lang);
  }
  
  // Initialize the app
  runApp(const MyApp());
  
  // Update widgets when app starts
  _updateWidgetsOnAppStart();
}

Future<void> _requestBatteryOptimizationExemption() async {
  try {
    const platform = MethodChannel(AppConstants.batteryOptChannel);
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
  
  // Force widgets to update on both platforms with the current language
  await widgetService.updateWithLastLocation();
  
  print('Widgets updated on app start');
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