CREATE TABLE cameras (
    id BINARY(16) PRIMARY KEY,
    zone_id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    stream_key_ref VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ONLINE', 'DEGRADED', 'OFFLINE')),
    last_frame_at DATETIME(6),
    config_version BIGINT NOT NULL,
    FOREIGN KEY (zone_id) REFERENCES parking_zones(id)
);
