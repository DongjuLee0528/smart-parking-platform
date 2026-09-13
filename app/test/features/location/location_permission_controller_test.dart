import 'package:flutter_test/flutter_test.dart';
import 'package:smart_parking_app/features/location/application/location_permission_controller.dart';

void main() {
  test('location permission controller type is available', () {
    expect(LocationPermissionController, isNotNull);
  });
}
