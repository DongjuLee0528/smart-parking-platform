import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:firebase_core/firebase_core.dart';

import 'features/authentication/presentation/pages/login_page.dart';
import 'shared/firebase/firebase_auth_boundary.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    await Firebase.initializeApp();
    runApp(
      ProviderScope(child: SmartParkingApp(auth: FirebaseAuthentication())),
    );
  } catch (_) {
    runApp(
      const MaterialApp(
        home: Scaffold(
          body: Center(child: Text('서비스를 시작하지 못했습니다. 잠시 후 앱을 다시 실행해 주세요.')),
        ),
      ),
    );
  }
}

class SmartParkingApp extends StatefulWidget {
  const SmartParkingApp({super.key, required this.auth});
  final FirebaseAuthBoundary auth;

  @override
  State<SmartParkingApp> createState() => _SmartParkingAppState();
}

class _SmartParkingAppState extends State<SmartParkingApp> {
  late final _authState = widget.auth.authStateChanges();

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Smart Parking',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF2563EB)),
      ),
      home: StreamBuilder<bool>(
        stream: _authState,
        builder: (context, snapshot) {
          if (snapshot.hasError) {
            return const Scaffold(
              body: Center(child: Text('인증 상태를 확인하지 못했습니다. 앱을 다시 실행해 주세요.')),
            );
          }
          if (!snapshot.hasData) {
            return const Scaffold(
              body: Center(child: CircularProgressIndicator()),
            );
          }
          return snapshot.data!
              ? HomePage(auth: widget.auth)
              : LoginPage(auth: widget.auth);
        },
      ),
    );
  }
}

class HomePage extends StatelessWidget {
  const HomePage({super.key, required this.auth});
  final FirebaseAuthBoundary auth;

  static const features = [
    'Authentication',
    'Location',
    'Nearby parking lots',
    'Parking-lot details',
    'Floor map',
    'Occupancy',
    'Saved parking location',
    'Notifications',
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Smart Parking'),
        actions: [
          TextButton(
            onPressed: () async {
              try {
                await auth.signOut();
              } catch (_) {
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('로그아웃하지 못했습니다. 다시 시도해 주세요.')),
                  );
                }
              }
            },
            child: const Text('로그아웃'),
          ),
        ],
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: features.length,
        itemBuilder: (context, index) => ListTile(title: Text(features[index])),
      ),
    );
  }
}
