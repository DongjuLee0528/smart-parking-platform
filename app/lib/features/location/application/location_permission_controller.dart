import 'package:geolocator/geolocator.dart';

class LocationPermissionController {
  const LocationPermissionController();

  Future<LocationPermission> request() async {
    final permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      return Geolocator.requestPermission();
    }
    return permission;
  }
}
