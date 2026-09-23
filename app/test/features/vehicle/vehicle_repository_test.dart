import 'package:flutter_test/flutter_test.dart';
import 'package:smart_parking_app/features/vehicle/domain/repositories/vehicle_repository.dart';

void main() {
  group('VehicleRepository', () {
    test('is declared', () {
      expect(VehicleRepository, isNotNull);
    });
  });
}
