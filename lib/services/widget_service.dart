// lib/services/widget_service.dart
import 'package:home_widget/home_widget.dart';
import '../constants/app_constants.dart';

class WidgetService {
  Future<void> initialize() async {
    await HomeWidget.setAppGroupId(AppConstants.appGroupId);
  }

  Future<void> updateWidgetsWithLocation(double latitude, double longitude) async {
    String data = "$latitude,$longitude";
    
    try {
      // Save location data for widgets
      await HomeWidget.saveWidgetData(AppConstants.dataKey, data);
      
      // Update iOS widget
      await HomeWidget.updateWidget(
        iOSName: AppConstants.iOSWidgetName
      );
      
      // Update Android widgets
      await HomeWidget.updateWidget(
        androidName: AppConstants.androidMediumWidgetName
      );
      await HomeWidget.updateWidget(
        androidName: AppConstants.androidSmallWidgetName
      );
      
      print("Widgets updated with location: $data");
    } catch (e) {
      print("Error updating widgets: $e");
    }
  }
}