import 'package:flutter/material.dart';
import '../services/location_service.dart';
import '../services/widget_service.dart';

class LocationPage extends StatefulWidget {
  @override
  _LocationPageState createState() => _LocationPageState();
}

class _LocationPageState extends State<LocationPage> with WidgetsBindingObserver {
  final LocationService _locationService = LocationService();
  final WidgetService _widgetService = WidgetService();
  String locationMessage = "Press the button to get location";
  String lang = "Eng"; // Default language
  
  // Track if we've updated widgets in this session
  bool _hasUpdatedWidgetsThisSession = false;

  @override
  void initState() {
    super.initState();
    // Register as an observer for app lifecycle changes
    WidgetsBinding.instance.addObserver(this);
    _initServices();
  }

  @override
  void dispose() {
    // Unregister observer when the page is disposed
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  // This method is called whenever the app lifecycle state changes
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && !_hasUpdatedWidgetsThisSession) {
      // App has come to the foreground - update widgets once per session
      print('App resumed - updating widgets');
      _getCurrentLocation();
      _hasUpdatedWidgetsThisSession = true;
    } else if (state == AppLifecycleState.paused) {
      // Reset the flag when app goes to background
      _hasUpdatedWidgetsThisSession = false;
    }
  }

  Future<void> _initServices() async {
    await _widgetService.initialize();
    setState(() {
      lang = _widgetService.currentLanguage;
    });
    _getCurrentLocation();
    _hasUpdatedWidgetsThisSession = true; // Mark as updated for this session
  }

  Future<void> _getCurrentLocation() async {
    try {
      final locationResult = await _locationService.getCurrentLocation();
      
      setState(() {
        if (locationResult.error != null) {
          locationMessage = locationResult.error!;
        } else if (locationResult.position != null) {
          print("Latitude: ${locationResult.position!.latitude}, Longitude: ${locationResult.position!.longitude}");
          locationMessage = "Latitude: ${locationResult.position!.latitude}, "
              "Longitude: ${locationResult.position!.longitude}";
          
          // Update widgets with new location and language
          _widgetService.updateWidgetsWithLocation(
            locationResult.position!.latitude, 
            locationResult.position!.longitude,
            language: lang
          );
        }
      });
    } catch (e) {
      setState(() {
        locationMessage = "Error getting location: $e";
      });
    }
  }

  Future<void> _changeLanguage(String newLang) async {
    if (newLang != lang) {
      await _widgetService.changeLanguage(newLang);
      setState(() {
        lang = newLang;
      });
      // Force widget update with new language
      _getCurrentLocation();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("Get Current Location")),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(locationMessage, textAlign: TextAlign.center),
            SizedBox(height: 20),
            // Language selection
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text("Language: "),
                DropdownButton<String>(
                  value: lang,
                  items: [
                    DropdownMenuItem(value: "Eng", child: Text("English")),
                    DropdownMenuItem(value: "ไทย", child: Text("Thai")),
                  ],
                  onChanged: (value) {
                    if (value != null) {
                      _changeLanguage(value);
                    }
                  },
                ),
              ],
            ),
            // Display current language for debugging
            Text("Current Language: $lang", 
                style: TextStyle(fontSize: 12, color: Colors.grey)),
            SizedBox(height: 20),
            ElevatedButton(
              onPressed: _getCurrentLocation,
              child: Text("Get Location"),
            ),
            SizedBox(height: 20),
            ElevatedButton(
              onPressed: () async {
                // Force immediate widget update
                await _widgetService.forceWidgetUpdate();
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('Widgets updated manually'))
                );
              },
              child: Text("Force Update Widgets"),
            ),
          ],
        ),
      ),
    );
  }
}