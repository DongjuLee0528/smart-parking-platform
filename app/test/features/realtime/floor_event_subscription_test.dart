import 'package:flutter_test/flutter_test.dart';
import 'package:smart_parking_app/features/realtime/domain/floor_event.dart';

void main() {
  test('floor event type is available', () {
    expect(FloorEvent, isNotNull);
  });
}
