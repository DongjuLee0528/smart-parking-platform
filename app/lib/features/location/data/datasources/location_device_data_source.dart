import 'package:geolocator/geolocator.dart';

import '../../domain/current_location.dart';

abstract interface class LocationDeviceDataSource {
  Future<LocationPermission> checkPermission();
  Future<bool> isServiceEnabled();
  Future<CurrentLocation> getCurrentLocation();
}

class GeolocatorLocationDeviceDataSource implements LocationDeviceDataSource {
  const GeolocatorLocationDeviceDataSource();

  @override
  Future<LocationPermission> checkPermission() => Geolocator.checkPermission();

  @override
  Future<bool> isServiceEnabled() => Geolocator.isLocationServiceEnabled();

  @override
  Future<CurrentLocation> getCurrentLocation() async {
    final position = await Geolocator.getCurrentPosition(
      locationSettings: const LocationSettings(
        accuracy: LocationAccuracy.low,
        timeLimit: Duration(seconds: 10),
      ),
    );
    return CurrentLocation(
      latitude: position.latitude,
      longitude: position.longitude,
    );
  }
}
