import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';

import '../../application/current_location_controller.dart';
import '../../application/location_permission_controller.dart';
import '../../domain/current_location.dart';

class LocationPermissionPrompt extends StatefulWidget {
  const LocationPermissionPrompt({
    super.key,
    this.controller = const LocationPermissionController(),
    this.currentLocation = const CurrentLocationController(),
  });

  final LocationPermissionController controller;
  final CurrentLocationController currentLocation;

  @override
  State<LocationPermissionPrompt> createState() => _LocationPermissionPromptState();
}

class _LocationPermissionPromptState extends State<LocationPermissionPrompt> {
  bool _requesting = false;
  String? _feedback;
  bool _locating = false;
  CurrentLocation? _position;
  CurrentLocationFailure? _locationFailure;

  Future<void> _request() async {
    if (_requesting || _locating) return;
    setState(() {
      _requesting = true;
      _feedback = null;
      _position = null;
      _locationFailure = null;
    });

    String feedback;
    var granted = false;
    try {
      final permission = await widget.controller.request();
      granted = permission == LocationPermission.whileInUse ||
          permission == LocationPermission.always;
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
    if (granted) await _loadLocation();
  }

  Future<void> _loadLocation() async {
    if (_locating || _requesting) return;
    setState(() {
      _locating = true;
      _position = null;
      _locationFailure = null;
    });

    CurrentLocation? position;
    CurrentLocationFailure? failure;
    try {
      position = await widget.currentLocation.load();
    } on CurrentLocationFailure catch (error) {
      failure = error;
    }
    if (!mounted) return;
    setState(() {
      _locating = false;
      _position = position;
      _locationFailure = failure;
    });
  }

  String _locationMessage(CurrentLocationFailure failure) => switch (failure) {
    CurrentLocationFailure.permissionDenied => '현재 위치를 확인하려면 위치 권한을 허용해 주세요.',
    CurrentLocationFailure.serviceDisabled => '기기의 위치 서비스를 켜 주세요.',
    CurrentLocationFailure.timeout => '현재 위치를 확인하는 데 시간이 너무 오래 걸렸어요.',
    CurrentLocationFailure.deviceError => '현재 위치를 확인하지 못했어요.',
  };

  @override
  Widget build(BuildContext context) {
    return ExpansionTile(
      title: const Text('Nearby parking lots'),
      childrenPadding: const EdgeInsets.all(16),
      children: [
        const Text('현재 위치는 가까운 주차장을 찾는 데 사용해요.'),
        TextButton(
          onPressed: _requesting || _locating ? null : _request,
          child: const Text('위치 권한 요청'),
        ),
        if (_feedback != null)
          Semantics(liveRegion: true, child: Text(_feedback!)),
        if (_locating) const LinearProgressIndicator(),
        if (_position != null)
          Semantics(liveRegion: true, child: const Text('현재 위치를 확인했어요.')),
        if (_locationFailure case final failure?) ...[
          Semantics(liveRegion: true, child: Text(_locationMessage(failure))),
          if (failure != CurrentLocationFailure.permissionDenied)
            TextButton(
              onPressed: _loadLocation,
              child: const Text('위치 조회 다시 시도'),
            ),
        ],
      ],
    );
  }
}
