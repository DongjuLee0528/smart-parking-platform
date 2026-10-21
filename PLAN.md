# ADM-MAP-001: floor drawing upload and replacement

## Goal and acceptance

- An authenticated ADMIN can upload a PNG, JPEG, or safe SVG drawing for an existing floor and replace it with an explicit expected map version. Non-admins cannot modify it.
- The server stores each accepted drawing as an immutable version in MySQL, serves the active drawing through an authenticated endpoint, and includes its URL and media type in the existing floor-map snapshot. Existing zone/space snapshot behavior remains intact.
- Invalid, empty, oversized, unsafe, or version-conflicting uploads receive the standard JSON error shape with appropriate 422/409/404 status. Each successful change is audited without logging drawing bytes or credentials.
- A small React upload component and API helper work with a caller-provided authenticated token. The admin login/session integration is explicitly out of scope because the existing auth UI is only a skeleton; the component is prepared for it but not represented as a complete logged-in web flow.
- Focused H2-backed API tests and frontend component tests pass; existing builds and tests remain green. Real MySQL and Firebase runtime remain unverified until configured.

## Approach

- Reuse `parking_floors.spaces_version` as the shared `mapVersion` counter; both parking-space saves and drawing changes advance it, keeping the existing snapshot contract. Add an immutable `map_versions` table with floor ID, version, media type, bytes, and timestamp; latest version is active. A maximum 5 MiB drawing keeps DB storage bounded for this MVP. The binary endpoint is protected by the existing Bearer auth. A later object store can replace blob storage without changing the upload/snapshot contract.
- Add `PUT /api/v1/admin/floors/{floorId}/map` as multipart with `expectedVersion` and `file`. Add `GET /api/v1/floors/{floorId}/map/drawing` to return the current bytes with a safe `Content-Type`, `nosniff`, and no caching. Keep the existing `GET /api/v1/floors/{floorId}/map` and add nullable drawing metadata.
- Validate file signature for PNG/JPEG, and parse SVG as XML with external entities/DTD disabled; reject active elements, event handlers, scripting links, and external references. Never render raw SVG inline in React; use image URL. Derive dimensions when straightforward; avoid trusting dimensions supplied by the client.
- Add optimistic CAS on the existing floor version and write audit metadata only. Ensure upload transaction does not overwrite an earlier version.
- React component accepts an auth token from its caller, uses existing `apiClient`, and shows selected floor ID, file, current version and clear success/error. It does not invent a production login path.

## Sources and trade-offs

- Notion ADM-MAP-001 requires admin SVG/image registration/replacement; the DB/ERD specifies immutable `map_versions`; API spec requires Bearer auth, mapVersion, 409/422/404 and audit.
- The repository has working BE-MAP-001 snapshot and BE-MAP-002 event flow, but its `FloorMap`/`MapVersion` and admin-web editor files are placeholders. `SecurityConfig` already protects `/api/v1/admin/**` by ADMIN role.
- Storing bounded drawings in the DB avoids filesystem/DB partial-commit mismatch and external storage configuration. It is a deliberate MVP trade-off; operational sizing/backup implications remain for real MySQL QA.
- Out of scope: Firebase admin-web login, visual polygon editing, object storage, CDN, map publication workflow, camera mapping.

## Proof

- `cd service-server && JAVA_HOME=/Users/dongjulee/Library/Java/JavaVirtualMachines/corretto-17.0.12/Contents/Home ./gradlew test`
- `cd admin-web && npm test -- --run && npm run build && npm run lint`
- Manual contract/security review of upload, drawing response headers, auth, version conflict and image rendering. No production MySQL/Firebase runtime verification is possible locally.
