import 'dart:async';

import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:smart_parking_app/main.dart';
import 'package:smart_parking_app/shared/firebase/firebase_auth_boundary.dart';

import '../../helpers/fake_auth.dart';

void main() {
  testWidgets('Google sign-in waits for the service before showing home', (tester) async {
    final auth = FakeAuth();
    final verification = Completer<void>();
    addTearDown(auth.changes.close);
    await tester.pumpWidget(
      SmartParkingApp(auth: auth, verifyUser: () => verification.future),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Google로 계속하기'));
    await tester.pump();
    await tester.pump();
    expect(auth.provider, LoginProvider.google);
    expect(find.text('Nearby parking lots'), findsNothing);

    verification.complete();
    await tester.pumpAndSettle();
    expect(find.text('Nearby parking lots'), findsOneWidget);
    await tester.tap(find.text('로그아웃'));
    await tester.pumpAndSettle();
    expect(find.text('Google로 계속하기'), findsOneWidget);
  });

  testWidgets('Apple sign-in reports a disabled account', (tester) async {
    final auth = FakeAuth()
      ..signInError = FirebaseAuthException(code: 'user-disabled');
    addTearDown(auth.changes.close);
    await tester.pumpWidget(SmartParkingApp(auth: auth));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Apple로 계속하기'));
    await tester.pumpAndSettle();
    expect(auth.provider, LoginProvider.apple);
    expect(find.text('사용이 중지된 계정입니다.'), findsOneWidget);
  });

  testWidgets('a service verification failure blocks home and can be retried', (tester) async {
    final auth = FakeAuth(signedIn: true);
    var attempts = 0;
    addTearDown(auth.changes.close);
    await tester.pumpWidget(SmartParkingApp(
      auth: auth,
      verifyUser: () async {
        if (++attempts == 1) throw StateError('Service unavailable');
      },
    ));
    await tester.pumpAndSettle();
    expect(find.text('Nearby parking lots'), findsNothing);
    await tester.tap(find.text('다시 시도'));
    await tester.pumpAndSettle();
    expect(find.text('Nearby parking lots'), findsOneWidget);
  });
}
