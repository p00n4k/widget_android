// lib/services/widget_service.dart
import 'package:home_widget/home_widget.dart';
import '../constants/app_constants.dart';
import 'package:geolocator/geolocator.dart';

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
      
      print("Widgets updated with location: $data at ${DateTime.now().toLocal()}");
    } catch (e) {
      print("Error updating widgets: $e");
    }
  }

  /// Force update widgets with last saved location
  Future<void> forceWidgetUpdate() async {
    try {
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