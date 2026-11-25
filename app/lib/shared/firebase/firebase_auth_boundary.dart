import 'package:firebase_auth/firebase_auth.dart';
import 'package:google_sign_in/google_sign_in.dart';

enum LoginProvider { google, apple }

abstract interface class FirebaseAuthBoundary {
  Stream<bool> authStateChanges();
  Future<void> signIn(LoginProvider provider);
  Future<void> signOut();
  Future<String?> getIdToken({bool forceRefresh = false});
}

class FirebaseAuthentication implements FirebaseAuthBoundary {
  FirebaseAuth get _auth => FirebaseAuth.instance;
  Future<void>? _googleInitialization;

  @override
  Stream<bool> authStateChanges() =>
      _auth.authStateChanges().map((user) => user != null);

  @override
  Future<void> signIn(LoginProvider provider) async {
    if (provider == LoginProvider.apple) {
      await _auth.signInWithProvider(AppleAuthProvider());
      return;
    }

    const clientId = String.fromEnvironment('GOOGLE_SIGN_IN_CLIENT_ID');
    const serverClientId =
        String.fromEnvironment('GOOGLE_SIGN_IN_SERVER_CLIENT_ID');
    final google = GoogleSignIn.instance;
    await (_googleInitialization ??= google.initialize(
      clientId: clientId.isEmpty ? null : clientId,
      serverClientId: serverClientId.isEmpty ? null : serverClientId,
    ));
    final account = await google.authenticate();
    final token = account.authentication.idToken;
    if (token == null || token.isEmpty) {
      throw StateError('Google did not return an ID token.');
    }
    await _auth.signInWithCredential(GoogleAuthProvider.credential(idToken: token));
  }

  @override
  Future<void> signOut() => _auth.signOut();

  @override
  Future<String?> getIdToken({bool forceRefresh = false}) async =>
      _auth.currentUser?.getIdToken(forceRefresh);
}
