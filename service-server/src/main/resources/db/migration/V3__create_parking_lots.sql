CREATE TABLE parking_lots (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500) NOT NULL,
    location geography(Point,4326) NOT NULL,
    operating_hours VARCHAR(2000) NOT NULL,
    fee_information VARCHAR(2000) NOT NULL,
    operation_status VARCHAR(255) NOT NULL CHECK (operation_status IN ('ACTIVE', 'INACTIVE')),
    setup_status VARCHAR(255) NOT NULL CHECK (setup_status IN ('DRAFT', 'GENERATING', 'REVIEW', 'VERIFIED', 'ACTIVE', 'REVIEW_REQUIRED')),
    version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX parking_lots_location_idx ON parking_lots USING GIST(location);
CREATE TABLE parking_floors (
    id UUID PRIMARY KEY,
    parking_lot_id UUID NOT NULL REFERENCES parking_lots(id),
    name VARCHAR(100) NOT NULL,
    floor_order INTEGER NOT NULL,
    UNIQUE (parking_lot_id, name)
);
CREATE TABLE parking_zones (
    id UUID PRIMARY KEY,
    floor_id UUID NOT NULL REFERENCES parking_floors(id),
    name VARCHAR(100) NOT NULL,
    UNIQUE (floor_id, name)
);
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    before_json TEXT,
    after_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
