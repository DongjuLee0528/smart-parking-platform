# Database

PostgreSQL 17 with PostGIS 3.5 is the primary service database.

Schema changes belong in Spring Boot Flyway migrations:

```text
service-server/src/main/resources/db/migration
```

Do not add Docker init scripts that duplicate Flyway-managed schema.
