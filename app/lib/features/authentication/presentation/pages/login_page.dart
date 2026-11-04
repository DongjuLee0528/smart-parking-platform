import '../../../../shared/firebase/firebase_auth_boundary.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key, required this.auth});
  final FirebaseAuthBoundary auth;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _form = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _signIn() async {
    if (_busy || !_form.currentState!.validate()) return;
    setState(() { _busy = true; _error = null; });
    try {
      await widget.auth.signIn(_email.text.trim(), _password.text);
    } on FirebaseAuthException catch (error) {
      if (!mounted) return;
      setState(() => _error = switch (error.code) {
        'user-disabled' => '사용이 중지된 계정입니다.',
        'network-request-failed' => '네트워크 연결을 확인하고 다시 시도해 주세요.',
        'too-many-requests' => '잠시 후 다시 시도해 주세요.',
        _ => '로그인하지 못했습니다. 이메일과 비밀번호를 확인해 주세요.',
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
      appBar: AppBar(title: const Text('로그인')),
      body: Center(child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 400),
          child: Form(key: _form, child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: _email, enabled: !_busy,
                decoration: const InputDecoration(labelText: '이메일'),
                keyboardType: TextInputType.emailAddress,
                autofillHints: const [AutofillHints.username],
                validator: (value) => value != null &&
                    RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$').hasMatch(value.trim())
                    ? null : '이메일을 입력해 주세요.',
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _password, enabled: !_busy, obscureText: true,
                decoration: const InputDecoration(labelText: '비밀번호'),
                autofillHints: const [AutofillHints.password],
                onFieldSubmitted: (_) => _signIn(),
                validator: (value) => value == null || value.isEmpty
                    ? '비밀번호를 입력해 주세요.' : null,
              ),
              const SizedBox(height: 24),
              if (_error != null) Semantics(
                liveRegion: true, child: Text(_error!),
              ),
              FilledButton(
                onPressed: _busy ? null : _signIn,
                child: Text(_busy ? '로그인 중…' : '로그인'),
              ),
            ],
          )),
        ),
      )),
    );
  }
}
