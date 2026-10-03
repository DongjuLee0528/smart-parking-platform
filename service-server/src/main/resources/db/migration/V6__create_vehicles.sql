CREATE TABLE vehicles (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    plate_number VARCHAR(20) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    is_primary BOOLEAN NOT NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE (user_id, plate_number),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
