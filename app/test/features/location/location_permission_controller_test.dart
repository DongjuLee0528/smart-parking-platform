import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:geolocator/geolocator.dart';
import 'package:smart_parking_app/features/location/application/location_permission_controller.dart';
import 'package:smart_parking_app/features/location/presentation/widgets/location_permission_prompt.dart';

class _FakeController extends LocationPermissionController {
  final result = Completer<LocationPermission>();
  int requests = 0;

  @override
  Future<LocationPermission> request() {
    requests++;
    return result.future;
  }
}

void main() {
  testWidgets('explains location use before explicitly requesting permission',
      (tester) async {
    final controller = _FakeController();
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(body: LocationPermissionPrompt(controller: controller)),
    ));
    expect(controller.requests, 0);

    await tester.tap(find.text('Nearby parking lots'));
    await tester.pumpAndSettle();
    expect(find.text('현재 위치는 가까운 주차장을 찾는 데 사용해요.'), findsOneWidget);
    expect(controller.requests, 0);

    await tester.tap(find.text('위치 권한 요청'));
    await tester.pump();
    expect(controller.requests, 1);
    expect(tester.widget<TextButton>(find.byType(TextButton)).onPressed, isNull);

    controller.result.complete(LocationPermission.whileInUse);
    await tester.pumpAndSettle();
    expect(find.text('위치 접근이 허용되었어요.'), findsOneWidget);
  });
}
