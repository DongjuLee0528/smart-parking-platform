import 'dart:async';
import 'package:smart_parking_app/shared/firebase/firebase_auth_boundary.dart';

class FakeAuth implements FirebaseAuthBoundary {
  FakeAuth({this.signedIn = false});
  bool signedIn;
  String? email;
  String? password;
  Object? signInError;
  bool refreshFails = false;
  int refreshes = 0;
  int signOuts = 0;
  String token = 'initial-token';
  final changes = StreamController<bool>.broadcast();

  @override
  Stream<bool> authStateChanges() async* {
    yield signedIn;
    yield* changes.stream;
  }

  @override
  Future<void> signIn(String email, String password) async {
    this.email = email;
    this.password = password;
    if (signInError != null) throw signInError!;
    signedIn = true;
    changes.add(true);
  }

  @override
  Future<void> signOut() async {
    signOuts++;
    signedIn = false;
    changes.add(false);
  }

  @override
  Future<String?> getIdToken({bool forceRefresh = false}) async {
    if (forceRefresh) {
      refreshes++;
      if (refreshFails) throw StateError('Refresh failed');
      token = 'refreshed-token';
    }
    return signedIn ? token : null;
  }
}
