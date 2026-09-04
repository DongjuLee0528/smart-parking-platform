# Smart Parking Platform

Baseline monorepo for a smart parking platform.

## Modules

| Module | Purpose |
| --- | --- |
| `app` | Flutter mobile app. Calls the Spring Boot API and Firebase services only. |
| `admin-web` | React admin console. Calls the Spring Boot API only. |
| `service-server` | Spring Boot service API, persistence, auth, public data sync, SSE, and internal AI APIs. |
| `ai-server` | FastAPI service for RTSP boundaries, inference, camera state, and occupancy analysis. |
| `ai-training` | Offline dataset, training, and evaluation CLI skeletons. |
| `database` | Database documentation and local verification scripts. Schema changes belong in Flyway. |
| `infrastructure` | Local Docker Compose infrastructure. |
| `docs` | Setup, architecture, and operations documentation. |
| `scripts` | Safe local verification scripts. |

## Prerequisites

- Java 17. The Spring module uses the bundled Gradle wrapper and can be run with `JAVA_HOME` pointing at Corretto 17.
- Node.js 24 LTS and npm 11.
- uv 0.12.9 or newer with CPython 3.12 available.
- Docker 28 and Docker Compose 2.
- FVM with Flutter 3.47.2 installed for the mobile app.

## Startup Order

1. Copy `.env.example` to `.env` and replace development values locally.
2. Start PostgreSQL/PostGIS: `docker compose up -d postgres`.
3. Run service migrations through Spring Boot/Flyway.
4. Start `service-server`.
5. Start `ai-server` if camera analysis is needed.
6. Start `admin-web` and run the Flutter app locally.

Flutter is not intended to run inside Docker.

## Verification

Run all safe baseline checks:

```bash
./scripts/verify-local.sh
```

Run the database-only local check:

```bash
./scripts/verify-postgis.sh
```

These scripts do not start RTSP streams, train models, download datasets, or call external public-data/Firebase/Kakao services.
