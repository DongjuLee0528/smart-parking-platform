CREATE TABLE saved_parking_locations (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    vehicle_id BINARY(16) NOT NULL,
    parking_space_id BINARY(16) NOT NULL,
    snapshot_json TEXT NOT NULL,
    saved_at DATETIME(6) NOT NULL,
    released_at DATETIME(6),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    FOREIGN KEY (parking_space_id) REFERENCES parking_spaces(id)
);

CREATE INDEX idx_saved_parking_locations_user_active
    ON saved_parking_locations(user_id, released_at);
