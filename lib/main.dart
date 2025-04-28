// lib/main.dart
import 'package:flutter/material.dart';
import 'package:home_widget/home_widget.dart';
import 'screens/location_page.dart';
import 'constants/app_constants.dart';
import 'services/widget_service.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Set up HomeWidget and trigger immediate widget update
  HomeWidget.setAppGroupId(AppConstants.appGroupId);
  
  // Initialize the app
  runApp(const MyApp());
  
  // Update widgets when app starts
  _updateWidgetsOnAppStart();
}

Future<void> _updateWidgetsOnAppStart() async {
  // Create WidgetService instance and request an immediate update
  final widgetService = WidgetService();
  await widgetService.initialize();
  
  // Force widgets to update on both platforms
  await HomeWidget.updateWidget(
    iOSName: AppConstants.iOSWidgetName,
    androidName: AppConstants.androidMediumWidgetName
  );
  
  await HomeWidget.updateWidget(
    androidName: AppConstants.androidSmallWidgetName
  );
  
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