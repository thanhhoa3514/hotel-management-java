-- Initial Database Schema
-- Version: V1
-- Description: Create initial tables for Hotel Management System
-- Matches db.sql structure with enum for ReservationStatus

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ===============================
-- Lookup Tables
-- ===============================

-- Room Types (lookup table)
CREATE TABLE IF NOT EXISTS room_types (
    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    price_per_night DECIMAL(10,2) NOT NULL
);

-- Room Statuses (lookup table)
CREATE TABLE IF NOT EXISTS room_statuses (
    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Services
CREATE TABLE IF NOT EXISTS services (
    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    price DECIMAL(10,2) NOT NULL
);

-- ===============================
-- Main Tables
-- ===============================

-- Rooms
CREATE TABLE IF NOT EXISTS rooms (
    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    room_type_id VARCHAR(36) NOT NULL REFERENCES room_types(id) ON DELETE RESTRICT,
    room_status_id VARCHAR(36) NOT NULL REFERENCES room_statuses(id) ON DELETE RESTRICT,
    floor SMALLINT,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Room Images
CREATE TABLE IF NOT EXISTS room_images (
    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    description TEXT,
    is_primary BOOLEAN DEFAULT FALSE,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Guests
CREATE TABLE IF NOT EXISTS guests (
    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    address TEXT,
    keycloak_user_id UUID UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Staff
CREATE TABLE IF NOT EXISTS staff (
    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    position VARCHAR(50),
    keycloak_user_id UUID UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Reservations (using enum for status instead of lookup table)
CREATE TABLE IF NOT EXISTS reservations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    guest_id VARCHAR(36) NOT NULL REFERENCES guests(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED'))
);

-- Reservation Rooms (Many-to-Many)
CREATE TABLE IF NOT EXISTS reservation_rooms (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reservation_id UUID NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE RESTRICT,
    UNIQUE(reservation_id, room_id)
);

-- ===============================
-- Indexes
-- ===============================

CREATE INDEX IF NOT EXISTS idx_guests_keycloak_user_id ON guests(keycloak_user_id);
CREATE INDEX IF NOT EXISTS idx_guests_email ON guests(email);
CREATE INDEX IF NOT EXISTS idx_rooms_room_number ON rooms(room_number);
CREATE INDEX IF NOT EXISTS idx_rooms_status ON rooms(room_status_id);
CREATE INDEX IF NOT EXISTS idx_reservations_guest ON reservations(guest_id);
CREATE INDEX IF NOT EXISTS idx_reservations_dates ON reservations(check_in, check_out);
CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations(status);
CREATE INDEX IF NOT EXISTS idx_reservation_rooms_reservation ON reservation_rooms(reservation_id);
CREATE INDEX IF NOT EXISTS idx_reservation_rooms_room ON reservation_rooms(room_id);

-- ===============================
-- Initial Data
-- ===============================

-- Room Statuses
INSERT INTO room_statuses (id, name) VALUES
    (uuid_generate_v4()::text, 'Available'),
    (uuid_generate_v4()::text, 'Occupied'),
    (uuid_generate_v4()::text, 'Maintenance'),
    (uuid_generate_v4()::text, 'Reserved'),
    (uuid_generate_v4()::text, 'Cleaning')
ON CONFLICT (name) DO NOTHING;

-- Room Types
INSERT INTO room_types (id, name, description, price_per_night) VALUES
    (uuid_generate_v4()::text, 'Standard', 'Cozy standard room with essential amenities', 80.00),
    (uuid_generate_v4()::text, 'Deluxe', 'Spacious deluxe room with city view', 150.00),
    (uuid_generate_v4()::text, 'Suite', 'Luxurious suite with separate living area', 280.00),
    (uuid_generate_v4()::text, 'Executive', 'Premium executive room with work desk', 200.00),
    (uuid_generate_v4()::text, 'Family', 'Large family room with extra beds', 220.00)
ON CONFLICT (name) DO NOTHING;

-- Services
INSERT INTO services (id, name, description, price) VALUES
    (uuid_generate_v4()::text, 'Room Service', '24/7 in-room dining', 25.00),
    (uuid_generate_v4()::text, 'Spa Treatment', 'Relaxing spa session', 80.00),
    (uuid_generate_v4()::text, 'Airport Transfer', 'Pick up and drop off service', 45.00),
    (uuid_generate_v4()::text, 'Laundry Service', 'Professional laundry and dry cleaning', 30.00),
    (uuid_generate_v4()::text, 'Extra Bed', 'Additional bed in room', 20.00)
ON CONFLICT (name) DO NOTHING;
