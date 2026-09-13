import 'package:flutter_test/flutter_test.dart';
import 'package:smart_parking_app/features/nearby_parking_lots/domain/repositories/nearby_parking_lots_repository.dart';

void main() {
  group('NearbyParkingLotsRepository', () {
    test('is declared', () {
      expect(NearbyParkingLotsRepository, isNotNull);
    });
  });
}
