import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:google_sign_in/google_sign_in.dart';

import '../../../../shared/firebase/firebase_auth_boundary.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key, required this.auth});
  final FirebaseAuthBoundary auth;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  bool _busy = false;
  String? _error;

  Future<void> _signIn(LoginProvider provider) async {
    if (_busy) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.auth.signIn(provider);
    } on GoogleSignInException catch (error) {
      if (mounted && error.code != GoogleSignInExceptionCode.canceled) {
        setState(() => _error = 'Google 로그인에 실패했습니다. 다시 시도해 주세요.');
      }
    } on FirebaseAuthException catch (error) {
      if (!mounted || error.code == 'web-context-canceled') return;
      setState(() => _error = switch (error.code) {
        'user-disabled' => '사용이 중지된 계정입니다.',
        'network-request-failed' => '네트워크 연결을 확인하고 다시 시도해 주세요.',
        'account-exists-with-different-credential' =>
          '이전에 사용한 로그인 방식으로 다시 시도해 주세요.',
        _ => '로그인하지 못했습니다. 다시 시도해 주세요.',
      });
    } catch (_) {
      if (mounted) setState(() => _error = '로그인하지 못했습니다. 다시 시도해 주세요.');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Smart Parking')),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text('주차, 더 가볍게'),
              const SizedBox(height: 24),
              FilledButton(
                onPressed: _busy ? null : () => _signIn(LoginProvider.google),
                child: const Text('Google로 계속하기'),
              ),
              OutlinedButton.icon(
                onPressed: _busy ? null : () => _signIn(LoginProvider.apple),
                icon: const Icon(Icons.apple),
                label: const Text('Apple로 계속하기'),
              ),
              if (_busy) const LinearProgressIndicator(),
              if (_error != null)
                Semantics(liveRegion: true, child: Text(_error!)),
            ],
          ),
        ),
      ),
    );
  }
}
