ALTER TABLE parking_floors ADD COLUMN spaces_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE parking_spaces (
    id BINARY(16) PRIMARY KEY,
    zone_id BINARY(16) NOT NULL,
    space_number VARCHAR(40) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('GENERAL', 'DISABLED', 'ELECTRIC', 'COMPACT')),
    map_polygon TEXT NOT NULL,
    active BOOLEAN NOT NULL,
    UNIQUE (zone_id, space_number),
    FOREIGN KEY (zone_id) REFERENCES parking_zones(id)
);

CREATE TABLE camera_space_mappings (
    parking_space_id BINARY(16) NOT NULL,
    camera_id BINARY(16) NOT NULL,
    image_polygon TEXT NOT NULL,
    priority INTEGER NOT NULL,
    config_version BIGINT NOT NULL,
    PRIMARY KEY (parking_space_id, camera_id),
    FOREIGN KEY (parking_space_id) REFERENCES parking_spaces(id),
    FOREIGN KEY (camera_id) REFERENCES cameras(id)
);
