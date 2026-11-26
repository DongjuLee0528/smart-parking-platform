import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:geolocator/geolocator.dart';
import 'package:smart_parking_app/features/location/application/current_location_controller.dart';
import 'package:smart_parking_app/features/location/data/datasources/location_device_data_source.dart';
import 'package:smart_parking_app/features/location/domain/current_location.dart';

class _Device implements LocationDeviceDataSource {
  LocationPermission permission = LocationPermission.whileInUse;
  bool enabled = true;
  Object? failure;
  int reads = 0;

  @override
  Future<LocationPermission> checkPermission() async => permission;

  @override
  Future<bool> isServiceEnabled() async => enabled;

  @override
  Future<CurrentLocation> getCurrentLocation() async {
    reads++;
    if (failure case final error?) throw error;
    return const CurrentLocation(latitude: 37.5, longitude: 127.0);
  }
}

void main() {
  late _Device device;
  late CurrentLocationController controller;

  setUp(() {
    device = _Device();
    controller = CurrentLocationController(device: device);
  });

  test('reads latitude and longitude after checking permission', () async {
    final location = await controller.load();
    expect(location.latitude, 37.5);
    expect(location.longitude, 127.0);
    expect(device.reads, 1);
  });

  for (final permission in [LocationPermission.denied, LocationPermission.deniedForever]) {
    test('does not read coordinates when permission is ${permission.name}', () async {
      device.permission = permission;
      await expectLater(controller.load(), throwsA(CurrentLocationFailure.permissionDenied));
      expect(device.reads, 0);
    });
  }

  test('does not read coordinates when location service is off', () async {
    device.enabled = false;
    await expectLater(controller.load(), throwsA(CurrentLocationFailure.serviceDisabled));
    expect(device.reads, 0);
  });

  test('reports a location timeout', () async {
    device.failure = TimeoutException('GPS timeout');
    await expectLater(controller.load(), throwsA(CurrentLocationFailure.timeout));
  });

  test('reports a device error', () async {
    device.failure = StateError('GPS failed');
    await expectLater(controller.load(), throwsA(CurrentLocationFailure.deviceError));
  });
}
