import 'dart:async';

import 'package:geolocator/geolocator.dart';

import '../data/datasources/location_device_data_source.dart';
import '../domain/current_location.dart';

enum CurrentLocationFailure implements Exception {
  permissionDenied,
  serviceDisabled,
  timeout,
  deviceError,
}

class CurrentLocationController {
  const CurrentLocationController({
    this.device = const GeolocatorLocationDeviceDataSource(),
  });

  final LocationDeviceDataSource device;

  Future<CurrentLocation> load() async {
    try {
      final permission = await device.checkPermission();
      if (permission != LocationPermission.whileInUse &&
          permission != LocationPermission.always) {
        throw CurrentLocationFailure.permissionDenied;
      }
      if (!await device.isServiceEnabled()) {
        throw CurrentLocationFailure.serviceDisabled;
      }
      return await device.getCurrentLocation();
    } on CurrentLocationFailure {
      rethrow;
    } on PermissionDeniedException {
      throw CurrentLocationFailure.permissionDenied;
    } on LocationServiceDisabledException {
      throw CurrentLocationFailure.serviceDisabled;
    } on TimeoutException {
      throw CurrentLocationFailure.timeout;
    } catch (_) {
      throw CurrentLocationFailure.deviceError;
    }
  }
}
