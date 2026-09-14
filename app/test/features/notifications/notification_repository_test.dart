import 'package:flutter_test/flutter_test.dart';
import 'package:smart_parking_app/features/notifications/domain/repositories/notification_repository.dart';

void main() {
  group('NotificationRepository', () {
    test('is declared', () {
      expect(NotificationRepository, isNotNull);
    });
  });
}
