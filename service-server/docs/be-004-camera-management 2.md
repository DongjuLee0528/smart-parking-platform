# BE-004: Camera management

Administrator endpoints require a verified Firebase Bearer token and the internal ADMIN role.
Responses wrap data in `data`; errors contain `code`, `message`, `traceId`, and `details`.

| Method | Path | Result |
| --- | --- | --- |
| POST | `/api/v1/admin/cameras` | 201, camera and Location header |
| PATCH | `/api/v1/admin/cameras/{cameraId}` | 200, updated camera |
| GET | `/api/v1/admin/cameras/{cameraId}` | 200, camera detail |
| GET | `/api/v1/admin/cameras` | 200, paged camera list |

Create requires `zoneId`, `name`, and `streamKeyRef`. The zone determines the floor and
parking lot; responses include all three IDs. `streamKeyRef` is an environment-variable
name such as `CAMERA_ENTRANCE_RTSP`, not a URL or credential. Never submit an RTSP URL,
username, password, or token through this API. The referenced secret must be configured
outside source control for the eventual AI integration.

PATCH requires the current `configVersion` plus the same configuration fields. A version
conflict returns 409. A missing camera returns 404, invalid input or an unknown zone
returns 422, and authentication/authorization failures return 401/403. Lists accept
`page` (default 0), `size` (default 20, maximum 100), and `sort` (`name,asc` or
`name,desc`). Administrator changes and before/after snapshots are audited in the same
transaction.

New or reconfigured cameras are `OFFLINE` with no last-frame time. No endpoint can
claim a camera is online from client input. RTSP connection tests, AI health updates,
camera-to-space mapping, and candidate generation require separate integration work.
H2 tests cover Flyway V3 and persistence without Docker; actual MySQL validation is
deferred until a MySQL server is available.
