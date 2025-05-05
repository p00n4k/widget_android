// lib/services/location_service.dart
import 'package:geolocator/geolocator.dart';

class LocationResult {
  final Position? position;
  final String? error;

  LocationResult({this.position, this.error});
}

class LocationService {
  Future<LocationResult> getCurrentLocation() async {
    // Check if location services are enabled
    bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      return LocationResult(error: "Location services are disabled.");
    }

    // Check location permission
    LocationPermission permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        return LocationResult(error: "Location permission denied.");
      }
    }

    if (permission == LocationPermission.deniedForever) {
      return LocationResult(
        error: "Location permissions are permanently denied."
      );
    }

    // Get current location
    try {
      Position position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        forceAndroidLocationManager: true,  // Force fresh location data
        timeLimit: Duration(seconds: 10),   // Add timeout to ensure fresh data
      );
      return LocationResult(position: position);
    } catch (e) {
      return LocationResult(error: "Error getting location: $e");
    }
  }
}