import 'package:flutter_test/flutter_test.dart';
import 'package:smart_parking_app/features/saved_parking_location/domain/saved_parking_location.dart';

void main() {
  test('saved parking location type is available', () {
    expect(SavedParkingLocation, isNotNull);
  });
}
