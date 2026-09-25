import 'package:flutter_test/flutter_test.dart';
import 'package:smart_parking_app/main.dart';

void main() {
  testWidgets('renders the app shell', (tester) async {
    await tester.pumpWidget(const SmartParkingApp());

    expect(find.text('Smart Parking'), findsOneWidget);
    expect(find.text('Nearby parking lots'), findsOneWidget);
  });
}
