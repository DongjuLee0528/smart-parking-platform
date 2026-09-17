# Migration File Plan

Spring Boot Flyway remains the single schema owner.

Planned service-server migration filenames:

- `V2__create_users_and_vehicles.sql`
- `V3__create_parking_lots_floors_zones_and_maps.sql`
- `V4__create_cameras_and_parking_spaces.sql`
- `V5__create_occupancy_current_and_history.sql`
- `V6__create_saved_parking_locations.sql`
- `V7__create_installation_jobs_and_audit_logs.sql`

Do not place duplicate schema migrations in this directory.
