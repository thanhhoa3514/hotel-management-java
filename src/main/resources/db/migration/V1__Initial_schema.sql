-- Initial Database Schema
-- Version: V1
-- Description: Create initial tables for Hotel Management System

-- Guest table
CREATE TABLE IF NOT EXISTS guest (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    keycloak_user_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on keycloak_user_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_guest_keycloak_user_id ON guest(keycloak_user_id);
CREATE INDEX IF NOT EXISTS idx_guest_email ON guest(email);

-- Room table
CREATE TABLE IF NOT EXISTS room (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_number VARCHAR(10) UNIQUE NOT NULL,
    room_type VARCHAR(50) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    description TEXT,
    max_guests INTEGER DEFAULT 2,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on room_number
CREATE INDEX IF NOT EXISTS idx_room_number ON room(room_number);
CREATE INDEX IF NOT EXISTS idx_room_status ON room(status);

-- Booking table (if needed in future)
CREATE TABLE IF NOT EXISTS booking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guest_id UUID NOT NULL REFERENCES guest(id) ON DELETE CASCADE,
    room_id UUID NOT NULL REFERENCES room(id) ON DELETE CASCADE,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for bookings
CREATE INDEX IF NOT EXISTS idx_booking_guest_id ON booking(guest_id);
CREATE INDEX IF NOT EXISTS idx_booking_room_id ON booking(room_id);
CREATE INDEX IF NOT EXISTS idx_booking_dates ON booking(check_in_date, check_out_date);

-- Service table (for hotel services)
CREATE TABLE IF NOT EXISTS service (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Booking Service junction table (many-to-many)
CREATE TABLE IF NOT EXISTS booking_service (
    booking_id UUID NOT NULL REFERENCES booking(id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES service(id) ON DELETE CASCADE,
    quantity INTEGER DEFAULT 1,
    subtotal DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (booking_id, service_id)
);

-- Comments for documentation
COMMENT ON TABLE guest IS 'Stores guest/customer information';
COMMENT ON TABLE room IS 'Hotel room inventory';
COMMENT ON TABLE booking IS 'Room bookings/reservations';
COMMENT ON TABLE service IS 'Hotel services (spa, restaurant, etc.)';
COMMENT ON TABLE booking_service IS 'Services added to bookings';

-- Initial data: Sample rooms
INSERT INTO room (id, room_number, room_type, price, status, description, max_guests) VALUES
    (gen_random_uuid(), '101', 'SINGLE', 50.00, 'AVAILABLE', 'Cozy single room with city view', 1),
    (gen_random_uuid(), '102', 'SINGLE', 50.00, 'AVAILABLE', 'Comfortable single room', 1),
    (gen_random_uuid(), '201', 'DOUBLE', 80.00, 'AVAILABLE', 'Spacious double room', 2),
    (gen_random_uuid(), '202', 'DOUBLE', 80.00, 'AVAILABLE', 'Modern double room with balcony', 2),
    (gen_random_uuid(), '301', 'SUITE', 150.00, 'AVAILABLE', 'Luxurious suite with ocean view', 4),
    (gen_random_uuid(), '302', 'SUITE', 150.00, 'AVAILABLE', 'Presidential suite', 4),
    (gen_random_uuid(), '401', 'DELUXE', 120.00, 'AVAILABLE', 'Deluxe room with modern amenities', 3),
    (gen_random_uuid(), '402', 'DELUXE', 120.00, 'AVAILABLE', 'Executive deluxe room', 3)
ON CONFLICT (room_number) DO NOTHING;

-- Initial data: Sample services
INSERT INTO service (id, name, description, price, category) VALUES
    (gen_random_uuid(), 'Room Service', '24/7 in-room dining', 25.00, 'DINING'),
    (gen_random_uuid(), 'Spa Treatment', 'Relaxing spa session', 80.00, 'WELLNESS'),
    (gen_random_uuid(), 'Airport Transfer', 'Pick up and drop off service', 45.00, 'TRANSPORT'),
    (gen_random_uuid(), 'Laundry Service', 'Professional laundry and dry cleaning', 30.00, 'AMENITY'),
    (gen_random_uuid(), 'Extra Bed', 'Additional bed in room', 20.00, 'ROOM')
ON CONFLICT DO NOTHING;
