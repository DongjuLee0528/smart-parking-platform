import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';

import '../../application/location_permission_controller.dart';

class LocationPermissionPrompt extends StatefulWidget {
  const LocationPermissionPrompt({
    super.key,
    this.controller = const LocationPermissionController(),
  });

  final LocationPermissionController controller;

  @override
  State<LocationPermissionPrompt> createState() => _LocationPermissionPromptState();
}

class _LocationPermissionPromptState extends State<LocationPermissionPrompt> {
  bool _requesting = false;
  String? _feedback;

  Future<void> _request() async {
    if (_requesting) return;
    setState(() {
      _requesting = true;
      _feedback = null;
    });

    String feedback;
    try {
      final permission = await widget.controller.request();
      feedback = switch (permission) {
        LocationPermission.whileInUse || LocationPermission.always =>
          '위치 접근이 허용되었어요.',
        LocationPermission.denied || LocationPermission.deniedForever =>
          '위치 접근이 허용되지 않았어요.',
        _ => '위치 권한을 확인하지 못했어요.',
      };
    } catch (_) {
      feedback = '위치 권한을 요청하지 못했어요. 다시 시도해 주세요.';
    }

    if (!mounted) return;
    setState(() {
      _requesting = false;
      _feedback = feedback;
    });
  }

  @override
  Widget build(BuildContext context) {
    return const SizedBox.shrink();
  }
}
