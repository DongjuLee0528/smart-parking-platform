# BE-006: AI occupancy-result ingestion

`POST /internal/v1/occupancy-results` accepts an AI result in the internal contract's
`eventId`, `cameraId`, `configVersion`, `model`, `capturedAt`, `processedAt`,
`cameraStatus`, and `spaces` shape. It returns 204 without a body. The AI service must
send `Authorization: Bearer <token>` using the externally configured
`AI_INTERNAL_TOKEN`. An unset token denies every request; Firebase user tokens do not
authenticate this endpoint. Do not commit or log the real token.

The submitted `configVersion` is the version of each camera-to-space mapping created
by BE-005, not the separate camera-record edit version. Every space must be active and
mapped to the submitted camera at that version. The service rejects unknown cameras
and spaces, duplicate spaces in one result, invalid confidence, and reversed capture
and processing times. An `eventId` already accepted for the same camera is a no-op;
reusing it for another camera returns 409. For each space, only an observation newer
than its current `observedAt` updates the current row. A history row is written only
when the state changes; the first observation is compared with `UNKNOWN`. Writes are
transactional, and space rows are locked in ID order to serialize concurrent results.

Authentication failure is 401, missing camera/space is 404, validation failure is
422, and concurrent or event-ID conflicts are 409. Errors use the shared JSON shape
with `code`, `message`, `traceId`, and `details`.

Flyway V5 adds `occupancy_current`, `occupancy_history`, and an event-ID deduplication
table in MySQL-compatible SQL. H2 tests exercise the ingestion contract and storage
without Docker or a real AI service. Actual MySQL migration and live AI integration
remain unverified. This slice does not perform AI inference, camera health updates,
SSE publication, retention cleanup, or Flutter rendering.
