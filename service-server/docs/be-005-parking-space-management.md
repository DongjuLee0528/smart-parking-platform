# BE-005: Parking space management

`PUT /api/v1/admin/floors/{floorId}/parking-spaces` saves a complete floor-space
configuration. It requires a verified Firebase Bearer token with the internal ADMIN role.
`GET` on the same path returns the current configuration for editing.

The PUT body contains the current floor `version` and a `spaces` array. Each item has
`zoneId`, `spaceNumber`, `type`, `active`, `mapPolygon`, and `cameraMappings`. New items
omit `id`; existing items include it. Omitted existing spaces are deactivated rather
than deleted. An empty array deactivates every space on the floor. The response wraps
`floorId`, the next `version`, and all spaces in `data`. A stale version returns 409.

`mapPolygon` contains app-map points. Each camera mapping contains a camera ID,
priority, and independent `imagePolygon` of CCTV-image points. Both polygons use
normalized x/y coordinates from 0 to 1, have at least three points and nonzero area.
Zones and cameras must belong to the requested floor. Space numbers must be unique
within a zone. The database preserves camera-to-space mappings separately from spaces.

Invalid input returns 422, missing floor/space returns 404, and uniqueness or version
conflicts return 409. Error bodies have `code`, `message`, `traceId`, and `details`.
The complete save is transactional and creates a floor-level audit event. The
authentication filter handles missing/invalid tokens as 401 and non-admin users as 403.

This slice does not determine EMPTY/OCCUPIED/UNKNOWN occupancy, create map files,
perform AI inference, or expose the user floor-map snapshot. H2 tests cover Flyway V4,
persistence, authorization, validation, and versioning without Docker. Actual MySQL
schema, constraints, and concurrent-write behavior remain for server QA.
