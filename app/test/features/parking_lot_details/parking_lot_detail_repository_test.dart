import 'package:flutter_test/flutter_test.dart';
import 'package:smart_parking_app/features/parking_lot_details/domain/repositories/parking_lot_detail_repository.dart';

void main() {
  group('ParkingLotDetailRepository', () {
    test('is declared', () {
      expect(ParkingLotDetailRepository, isNotNull);
    });
  });
}
