CREATE TABLE parking_lots (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    operating_hours VARCHAR(2000) NOT NULL,
    fee_information VARCHAR(2000) NOT NULL,
    operation_status VARCHAR(255) NOT NULL CHECK (operation_status IN ('ACTIVE', 'INACTIVE')),
    setup_status VARCHAR(255) NOT NULL CHECK (setup_status IN ('DRAFT', 'GENERATING', 'REVIEW', 'VERIFIED', 'ACTIVE', 'REVIEW_REQUIRED')),
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);
CREATE TABLE parking_floors (
    id BINARY(16) PRIMARY KEY,
    parking_lot_id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    floor_order INTEGER NOT NULL,
    UNIQUE (parking_lot_id, name),
    FOREIGN KEY (parking_lot_id) REFERENCES parking_lots(id)
);
CREATE TABLE parking_zones (
    id BINARY(16) PRIMARY KEY,
    floor_id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    UNIQUE (floor_id, name),
    FOREIGN KEY (floor_id) REFERENCES parking_floors(id)
);
CREATE TABLE audit_logs (
    id BINARY(16) PRIMARY KEY,
    actor_id BINARY(16) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BINARY(16) NOT NULL,
    before_json TEXT,
    after_json TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    FOREIGN KEY (actor_id) REFERENCES users(id)
);
