CREATE TABLE occupancy_current (
    parking_space_id BINARY(16) PRIMARY KEY,
    state VARCHAR(20) NOT NULL CHECK (state IN ('EMPTY', 'OCCUPIED', 'UNKNOWN')),
    confidence DOUBLE NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    source_camera_id BINARY(16) NOT NULL,
    observed_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    FOREIGN KEY (parking_space_id) REFERENCES parking_spaces(id),
    FOREIGN KEY (source_camera_id) REFERENCES cameras(id)
);

CREATE INDEX idx_occupancy_current_state_observed ON occupancy_current(state, observed_at);

CREATE TABLE occupancy_history (
    id BINARY(16) PRIMARY KEY,
    parking_space_id BINARY(16) NOT NULL,
    from_state VARCHAR(20) NOT NULL CHECK (from_state IN ('EMPTY', 'OCCUPIED', 'UNKNOWN')),
    to_state VARCHAR(20) NOT NULL CHECK (to_state IN ('EMPTY', 'OCCUPIED', 'UNKNOWN')),
    confidence DOUBLE NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    observed_at DATETIME(6) NOT NULL,
    FOREIGN KEY (parking_space_id) REFERENCES parking_spaces(id)
);

CREATE INDEX idx_occupancy_history_space_observed ON occupancy_history(parking_space_id, observed_at);

CREATE TABLE occupancy_ingest_events (
    event_id BINARY(16) PRIMARY KEY,
    camera_id BINARY(16) NOT NULL,
    captured_at DATETIME(6) NOT NULL,
    FOREIGN KEY (camera_id) REFERENCES cameras(id)
);
