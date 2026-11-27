import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:geolocator/geolocator.dart';
import 'package:smart_parking_app/features/location/application/current_location_controller.dart';
import 'package:smart_parking_app/features/location/application/location_permission_controller.dart';
import 'package:smart_parking_app/features/location/domain/current_location.dart';
import 'package:smart_parking_app/features/location/presentation/widgets/location_permission_prompt.dart';

class _Permission extends LocationPermissionController {
  final result = Completer<LocationPermission>();

  @override
  Future<LocationPermission> request() => result.future;
}

class _Location extends CurrentLocationController {
  final first = Completer<CurrentLocation>();
  int requests = 0;

  @override
  Future<CurrentLocation> load() {
    requests++;
    if (requests == 1) return first.future;
    return Future.value(const CurrentLocation(latitude: 37.5, longitude: 127.0));
  }
}

Future<void> _open(WidgetTester tester, _Permission permission, _Location location) async {
  await tester.pumpWidget(MaterialApp(
    home: Scaffold(body: LocationPermissionPrompt(
      controller: permission,
      currentLocation: location,
    )),
  ));
  await tester.tap(find.text('Nearby parking lots'));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('waits for permission and retries a failed lookup once tapped', (tester) async {
    final permission = _Permission();
    final location = _Location();
    await _open(tester, permission, location);
    expect(location.requests, 0);

    final permissionButton = find.widgetWithText(TextButton, '위치 권한 요청');
    await tester.tap(permissionButton);
    await tester.pump();
    expect(location.requests, 0);

    permission.result.complete(LocationPermission.whileInUse);
    await tester.pump();
    expect(location.requests, 1);
    expect(tester.widget<TextButton>(permissionButton).onPressed, isNull);

    location.first.completeError(CurrentLocationFailure.serviceDisabled);
    await tester.pumpAndSettle();
    expect(find.text('기기의 위치 서비스를 켜 주세요.'), findsOneWidget);
    await tester.tap(find.text('위치 조회 다시 시도'));
    await tester.pumpAndSettle();
    expect(location.requests, 2);
    expect(find.text('현재 위치를 확인했어요.'), findsOneWidget);
  });

  testWidgets('does not look up coordinates when permission is denied', (tester) async {
    final permission = _Permission();
    final location = _Location();
    await _open(tester, permission, location);
    await tester.tap(find.text('위치 권한 요청'));
    permission.result.complete(LocationPermission.deniedForever);
    await tester.pumpAndSettle();
    expect(location.requests, 0);
    expect(find.text('위치 접근이 허용되지 않았어요.'), findsOneWidget);
  });
}
