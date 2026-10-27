CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE IF NOT EXISTS user_role AS ENUM ('ADMIN', 'CUSTOMER', 'USER');

CREATE TABLE IF NOT EXISTS users (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       name VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255),
                       role VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);