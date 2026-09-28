CREATE TABLE users (
    id BINARY(16) PRIMARY KEY,
    firebase_uid VARCHAR(128) NOT NULL UNIQUE,
    email VARCHAR(320) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);
