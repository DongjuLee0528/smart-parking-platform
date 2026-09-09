# Development Baseline

## Local Services

1. `docker compose up -d postgres`
2. `cd service-server && JAVA_HOME=/Users/dongjulee/Library/Java/JavaVirtualMachines/corretto-17.0.12/Contents/Home ./gradlew bootRun --args='--spring.profiles.active=local'`
3. `cd ai-server && uv run uvicorn app.main:app --reload`
4. `cd admin-web && npm run dev`
5. `cd app && fvm flutter run`

## Required Local Files

- `app/android/app/google-services.json`
- `app/ios/Runner/GoogleService-Info.plist`
- Firebase Admin SDK JSON outside the repository, referenced by `FIREBASE_CREDENTIALS_PATH`
- Kakao platform keys supplied through local app configuration

Do not commit credentials, generated model weights, datasets, runs, checkpoints, or local environment files.
