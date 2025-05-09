// lib/main.dart
import 'package:flutter/material.dart';
import 'package:home_widget/home_widget.dart';
import 'screens/location_page.dart';
import 'constants/app_constants.dart';
import 'services/widget_service.dart';
import 'dart:io';
import 'package:flutter/services.dart';

// Global variable for language preference
String lang = "Eng"; // Default to English

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Set up HomeWidget and trigger immediate widget update
  HomeWidget.setAppGroupId(AppConstants.appGroupId);
  
  // Request battery optimization exception on Android
  if (Platform.isAndroid) {
    _requestBatteryOptimizationExemption();
  }
  
  // Initialize the app
  runApp(const MyApp());
  
  // Update widgets when app starts
  _updateWidgetsOnAppStart();
}

Future<void> _requestBatteryOptimizationExemption() async {
  try {
    const platform = MethodChannel('com.example.test_wid_and/battery_optimization');
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
  
  // Get the current language setting
  lang = widgetService.currentLanguage;
  print('Current language from widget service: $lang');
  
  // Update widgets based on platform
  if (Platform.isIOS) {
    await HomeWidget.updateWidget(
      iOSName: AppConstants.iOSWidgetName,
    );
    print('iOS widget updated on app start');
  }
  
  if (Platform.isAndroid) {
    // Android widgets - update each one separately
    await HomeWidget.updateWidget(
      androidName: AppConstants.androidMediumWidgetName
    );
    
    await HomeWidget.updateWidget(
      androidName: AppConstants.androidSmallWidgetName
    );
    print('Android widgets updated on app start');
  }
  
  print('Widgets updated on app start with language: $lang');
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