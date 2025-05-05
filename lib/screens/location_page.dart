import 'package:flutter/material.dart';
import '../services/location_service.dart';
import '../services/widget_service.dart';
import '../services/language_service.dart';
import '../main.dart' as main;

class LocationPage extends StatefulWidget {
  @override
  _LocationPageState createState() => _LocationPageState();
}

class _LocationPageState extends State<LocationPage> with WidgetsBindingObserver {
  final LocationService _locationService = LocationService();
  final WidgetService _widgetService = WidgetService();
  final LanguageService _languageService = LanguageService();
  String locationMessage = "Press the button to get location";
  String selectedLanguage = main.lang;  // Initialize with current language
  
  // Track if we've updated widgets in this session
  bool _hasUpdatedWidgetsThisSession = false;

  @override
  void initState() {
    super.initState();
    // Register as an observer for app lifecycle changes
    WidgetsBinding.instance.addObserver(this);
    Future.microtask(() => _initServices());
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
    await _languageService.initialize();
    
    // Get the current language from shared preferences
    String savedLanguage = await _languageService.getCurrentLanguage();
    setState(() {
      selectedLanguage = savedLanguage;
    });
    
    _getCurrentLocation();
    _hasUpdatedWidgetsThisSession = true;
  }

  Future<void> _getCurrentLocation() async {
    try {
      final locationResult = await _locationService.getCurrentLocation();
      
      setState(() {
        if (locationResult.error != null) {
          locationMessage = locationResult.error!;
        } else if (locationResult.position != null) {
          locationMessage = "Latitude: ${locationResult.position!.latitude}, "
              "Longitude: ${locationResult.position!.longitude}";
          
          // Update widgets with new location
          _widgetService.updateWidgetsWithLocation(
            locationResult.position!.latitude, 
            locationResult.position!.longitude
          );
        }
      });
    } catch (e) {
      setState(() {
        locationMessage = "Error getting location: $e";
      });
    }
  }

  Future<void> _applyLanguageChange() async {
    // Update the global language variable
    main.lang = selectedLanguage;
    
    // Update language using the service
    await _languageService.updateLanguage(selectedLanguage);
    
    // Force widgets to update with new language
    await _widgetService.updateWithLastLocation();
    
    // Show confirmation to user
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Language changed to: $selectedLanguage'))
    );
    
    // Refresh UI to show the language change
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("Get Current Location")),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // Display current language for debugging
            Text("Current Language: $selectedLanguage", 
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            SizedBox(height: 10),
            
            // Language dropdown
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text("Select Language: "),
                SizedBox(width: 10),
                DropdownButton<String>(
                  value: selectedLanguage,
                  items: <String>['Eng', 'ไทย']
                      .map<DropdownMenuItem<String>>((String value) {
                    return DropdownMenuItem<String>(
                      value: value,
                      child: Text(value),
                    );
                  }).toList(),
                  onChanged: (String? newValue) {
                    setState(() {
                      selectedLanguage = newValue!;
                    });
                  },
                ),
                SizedBox(width: 10),
                ElevatedButton(
                  onPressed: _applyLanguageChange,
                  child: Text("Apply"),
                ),
              ],
            ),
            
            SizedBox(height: 20),
            Text(locationMessage, textAlign: TextAlign.center),
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