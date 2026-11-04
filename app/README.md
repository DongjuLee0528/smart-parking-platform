# Smart Parking App

Flutter mobile baseline pinned with FVM.

## Local Setup

```bash
fvm flutter pub get
fvm flutter analyze
fvm flutter test
```

## Local Credentials

Add these files locally. Do not commit them:

- `android/app/google-services.json`
- `ios/Runner/GoogleService-Info.plist`

Kakao Map credentials must stay behind app configuration and platform setup. No real Kakao key is committed in this baseline.

## APP-001: Firebase login

The app supports email/password sign-in, session restoration and sign-out.
Enable Email/Password in Firebase Authentication and create a test account in
the same project used by the Spring Boot Firebase Admin configuration.
Registration and password reset are outside this task.

Android: register the application ID from `android/app/build.gradle.kts` and
place `google-services.json` in `android/app/`. The Google Services plugin is
applied when this file exists. Without Firebase configuration, startup shows
an error instead of opening an unauthenticated home screen.

iOS: register the Runner bundle identifier and add `GoogleService-Info.plist`
to the Runner target using Xcode (including target membership).
This implementation uses native Firebase configuration on Android/iOS;
web and desktop Firebase setup is not included.

Run with your service address (Android emulator uses `10.0.2.2` for the host):

```bash
fvm flutter run --dart-define=API_BASE_URL=https://your-service.example/api/v1
fvm flutter test test/features/authentication test/widget_test.dart
```

Use the shared `apiClient` for service requests. It obtains ID tokens from the
Firebase SDK rather than storing tokens separately, retries a 401 once after
forced refresh, and signs out if refresh fails or the retry is unauthorized.
A 403 is returned to the caller without ending the session. Requests to other
origins/paths and automatic redirects are blocked to protect the bearer token.

Setup reference: https://firebase.google.com/docs/auth/flutter/password-auth
