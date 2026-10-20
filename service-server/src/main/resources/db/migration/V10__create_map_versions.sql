CREATE TABLE map_versions (
    id BINARY(16) PRIMARY KEY,
    floor_id BINARY(16) NOT NULL,
    version BIGINT NOT NULL,
    media_type VARCHAR(30) NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    content LONGBLOB NOT NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE (floor_id, version),
    FOREIGN KEY (floor_id) REFERENCES parking_floors(id)
);
