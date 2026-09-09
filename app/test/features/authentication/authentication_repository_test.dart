import 'package:flutter_test/flutter_test.dart';
import 'package:smart_parking_app/features/authentication/domain/repositories/authentication_repository.dart';

void main() {
  group('AuthenticationRepository', () {
    test('is declared', () {
      expect(AuthenticationRepository, isNotNull);
    });
  });
}
