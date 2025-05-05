// lib/constants/app_constants.dart
class AppConstants {
  // App Group ID for widget communication
  static const String appGroupId = "group.homescreenaapp";
  
  // Widget names
  static const String iOSWidgetName = "MyHomeWidget";
  static const String androidMediumWidgetName = "widget.MyHomeMediumWidget";
  static const String androidSmallWidgetName = "widget.MyHomeSmallWidget";
  
  // Data keys
  static const String appLocationData = "AppLocationData";
  static const String appLanguageData = "AppLanguageData";

  // Method channels
  static const String batteryOptChannel = "com.check_phoon.android_widget/battery_optimization";
  static const String languageChannel = "com.check_phoon.android_widget/language";
}