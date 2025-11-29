-- Create Keycloak database (required for Keycloak to work)
CREATE DATABASE keycloak;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO hoteluser;

-- Enable UUID extension for hotelmanagement database
\c hotelmanagement;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

