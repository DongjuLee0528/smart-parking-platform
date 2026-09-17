# Infrastructure

Local Docker Compose lives at the repository root in `compose.yaml`.

PostgreSQL/PostGIS is the only database service in the baseline. Flyway migrations in `service-server/src/main/resources/db/migration` own schema changes.

Verify PostGIS locally:

```bash
./scripts/verify-postgis.sh
```
