import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:smart_parking_app/main.dart';
import '../../helpers/fake_auth.dart';

void main() {
  testWidgets('validates, signs in and signs out', (tester) async {
    final auth = FakeAuth();
    addTearDown(auth.changes.close);
    await tester.pumpWidget(SmartParkingApp(auth: auth));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(FilledButton));
    await tester.pump();
    expect(find.text('이메일을 입력해 주세요.'), findsOneWidget);
    expect(auth.email, isNull);
    await tester.enterText(find.byType(TextFormField).at(0), ' ain@example.com ');
    await tester.enterText(find.byType(TextFormField).at(1), ' secret ');
    await tester.tap(find.byType(FilledButton));
    await tester.pumpAndSettle();
    expect(auth.email, 'ain@example.com');
    expect(auth.password, ' secret ');
    expect(find.text('Nearby parking lots'), findsOneWidget);
    await tester.tap(find.text('로그아웃'));
    await tester.pumpAndSettle();
    expect(find.byType(TextFormField), findsNWidgets(2));
  });

  testWidgets('shows disabled-account error and allows retry', (tester) async {
    final auth = FakeAuth()..signInError = FirebaseAuthException(code: 'user-disabled');
    addTearDown(auth.changes.close);
    await tester.pumpWidget(SmartParkingApp(auth: auth));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextFormField).at(0), 'ain@example.com');
    await tester.enterText(find.byType(TextFormField).at(1), 'secret');
    await tester.tap(find.byType(FilledButton));
    await tester.pumpAndSettle();
    expect(find.text('사용이 중지된 계정입니다.'), findsOneWidget);
    expect(tester.widget<FilledButton>(find.byType(FilledButton)).onPressed, isNotNull);
  });
}
