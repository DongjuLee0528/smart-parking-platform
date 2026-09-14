import 'package:flutter_test/flutter_test.dart';
import 'package:smart_parking_app/features/occupancy/domain/repositories/occupancy_repository.dart';

void main() {
  group('OccupancyRepository', () {
    test('is declared', () {
      expect(OccupancyRepository, isNotNull);
    });
  });
}
