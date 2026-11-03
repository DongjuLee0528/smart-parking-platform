import 'package:firebase_auth/firebase_auth.dart';

abstract interface class FirebaseAuthBoundary {
  Stream<bool> authStateChanges();
  Future<void> signIn(String email, String password);
  Future<void> signOut();
  Future<String?> getIdToken({bool forceRefresh = false});
}

class FirebaseAuthentication implements FirebaseAuthBoundary {
  FirebaseAuth get _auth => FirebaseAuth.instance;

  @override
  Stream<bool> authStateChanges() =>
      _auth.authStateChanges().map((user) => user != null);

  @override
  Future<void> signIn(String email, String password) async {
    await _auth.signInWithEmailAndPassword(email: email, password: password);
  }

  @override
  Future<void> signOut() => _auth.signOut();

  @override
  Future<String?> getIdToken({bool forceRefresh = false}) async =>
      _auth.currentUser?.getIdToken(forceRefresh);
}
