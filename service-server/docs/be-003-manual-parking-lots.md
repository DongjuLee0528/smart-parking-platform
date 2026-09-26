# BE-003: Manual parking lot management

This slice supports manual operation without an external parking data provider.
All endpoints require a verified Firebase Bearer token. Administrator endpoints
require the internal user's ADMIN role. Responses wrap resource data in `data`.

## Endpoints

| Method | Path | Access | Result |
| --- | --- | --- | --- |
| POST | /api/v1/admin/parking-lots | ADMIN | 201, resource and Location header |
| PATCH | /api/v1/admin/parking-lots/{lotId} | ADMIN | 200, updated resource |
| GET | /api/v1/admin/parking-lots | ADMIN | 200, all states |
| GET | /api/v1/admin/parking-lots/{lotId} | ADMIN | 200, including drafts |
| GET | /api/v1/parking-lots | USER/ADMIN | 200, ACTIVE lots only |
| GET | /api/v1/parking-lots/{lotId} | USER/ADMIN | 200, ACTIVE lot only |

Lists accept `page` (default 0), `size` (default 20, maximum 100), and
`sort` (`name,asc` or `name,desc`). The response contains `items`, `page`,
`size`, and `totalElements`. UUID ordering breaks equal-name ties.
This basic list is not the separately specified PostGIS nearby-search API.

## Creation

```json
{
  "name": "Example parking lot",
  "address": "Example address",
  "latitude": 37.5,
  "longitude": 127.0,
  "operatingHours": "09:00-18:00",
  "feeInformation": "See on-site pricing",
  "floors": [
    {"name": "B1", "floorOrder": -1, "zones": ["A", "B"]}
  ]
}
```

All fields are required. Operating hours and fee information may be empty strings
when unknown. Floors may be an empty list. Each floor requires a name, order,
and zones list; zone names must be nonblank. Names are unique within their parent
after trimming. Each nested list is limited to 100 entries.
Coordinates use WGS84; longitude is stored as X and latitude as Y.

New lots are INACTIVE/DRAFT. General information edits cannot activate a lot or
bypass installation verification. Administrator reads allow manual data management
before activation; app reads will not expose these drafts. Activation, floor/zone
editing, maps and installation verification belong to their follow-up features.
Initial floor/zone records have persistent UUIDs and are retained on lot updates.
There is no physical deletion endpoint in this slice.

## Update and errors

PATCH replaces the six basic information fields from the creation request and
requires the last returned `version`. It does not change floors, zones or lifecycle
states. Concurrent updates use JPA optimistic locking; stale versions return 409.
Administrator create/update writes and their before/after JSON audit snapshots are
in the same database transaction. The actor ID comes from the authenticated principal.

Errors use `code`, `message`, `traceId`, and `details`:
401 authentication failure, 403 insufficient role, 404 unknown or nonpublic lot,
409 version/data conflict, and 422 invalid body/query/path input.

## Verification and follow-up

The focused service and MVC tests use repository and token-verifier doubles,
not a production authentication bypass. They require no Docker or database.
Production persistence requires PostgreSQL/PostGIS and Flyway migrations V1-V3.
Real migrations, spatial persistence and concurrent database transactions must
also be verified against PostgreSQL/PostGIS before deployment; mocks do not prove them.

External provider integration and automatic collection are intentionally absent.
After selecting a provider, determine authentication, pagination, normalization,
refresh frequency, deduplication and manual-edit precedence. Keep provider keys in
external configuration and exclude local secret files from Git.
