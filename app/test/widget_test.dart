import 'package:flutter_test/flutter_test.dart';
import 'package:smart_parking_app/main.dart';
import 'helpers/fake_auth.dart';

void main() {
  testWidgets('renders the app shell', (tester) async {
    final auth = FakeAuth(signedIn: true);
    addTearDown(auth.changes.close);
    await tester.pumpWidget(SmartParkingApp(auth: auth));
    await tester.pumpAndSettle();

    expect(find.text('Smart Parking'), findsOneWidget);
    expect(find.text('Nearby parking lots'), findsOneWidget);
  });
}
