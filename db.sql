CREATE TABLE IF NOT EXISTS room_types (
                                          id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    price_per_night DECIMAL(10,2) NOT NULL
    );

-- Room Statuses
CREATE TABLE IF NOT EXISTS room_statuses (
                                             id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE
    );

-- Reservation Statuses
CREATE TABLE IF NOT EXISTS reservation_statuses (
                                                    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE
    );

-- Payment Statuses
CREATE TABLE IF NOT EXISTS payment_statuses (
                                                id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE
    );

-- Services
CREATE TABLE IF NOT EXISTS services (
                                        id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    price DECIMAL(10,2) NOT NULL
    );

-- ===============================
-- Main Tables
-- ===============================

-- Rooms
CREATE TABLE IF NOT EXISTS rooms (
                                     id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    room_number VARCHAR(20) NOT NULL UNIQUE,
    room_type_id VARCHAR(36) NOT NULL REFERENCES room_types(id) ON DELETE RESTRICT,
    room_status_id VARCHAR(36) NOT NULL REFERENCES room_statuses(id) ON DELETE RESTRICT,
    floor INTEGER,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Room Images
CREATE TABLE IF NOT EXISTS room_images (
                                           id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    description TEXT,
    is_primary BOOLEAN DEFAULT FALSE,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Guests
CREATE TABLE IF NOT EXISTS guests (
                                      id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
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
                                     id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    position VARCHAR(50),
    keycloak_user_id UUID UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Reservations
CREATE TABLE IF NOT EXISTS reservations (
                                            id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    guest_id VARCHAR(36) NOT NULL REFERENCES guests(id) ON DELETE CASCADE,
    status_id VARCHAR(36) NOT NULL REFERENCES reservation_statuses(id) ON DELETE RESTRICT,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Reservation Rooms (Many-to-Many)
CREATE TABLE IF NOT EXISTS reservation_rooms (
                                                 id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    reservation_id VARCHAR(36) NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE RESTRICT,
    UNIQUE(reservation_id, room_id)
    );

-- Reservation Staff (Many-to-Many)
CREATE TABLE IF NOT EXISTS reservation_staff (
                                                 id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    reservation_id VARCHAR(36) NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    staff_id VARCHAR(36) NOT NULL REFERENCES staff(id) ON DELETE RESTRICT,
    UNIQUE(reservation_id, staff_id)
    );

-- Reservation Services (Many-to-Many with quantity and auto total_price)
CREATE TABLE IF NOT EXISTS reservation_services (
                                                    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    reservation_id VARCHAR(36) NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    service_id VARCHAR(36) NOT NULL REFERENCES services(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL DEFAULT 1,
    total_price DECIMAL(10,2) NOT NULL
    );

-- Payments
CREATE TABLE IF NOT EXISTS payments (
                                        id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    reservation_id VARCHAR(36) NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    status_id VARCHAR(36) NOT NULL REFERENCES payment_statuses(id) ON DELETE RESTRICT,
    amount DECIMAL(10,2) NOT NULL,
    method VARCHAR(50) NOT NULL,
    transaction_code VARCHAR(100),
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Invoices
CREATE TABLE IF NOT EXISTS invoices (
                                        id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    reservation_id VARCHAR(36) NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    payment_id VARCHAR(36) REFERENCES payments(id) ON DELETE SET NULL,
    staff_id VARCHAR(36) REFERENCES staff(id) ON DELETE SET NULL,
    issue_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL,
    tax DECIMAL(10,2) DEFAULT 0,
    discount DECIMAL(10,2) DEFAULT 0,
    final_amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- ===============================
-- Invoice Details: split into 2 tables
-- ===============================

-- Invoice Rooms
CREATE TABLE IF NOT EXISTS invoice_rooms (
                                             id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id VARCHAR(36) NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE RESTRICT,
    description TEXT,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL
    );

-- Invoice Services
CREATE TABLE IF NOT EXISTS invoice_services (
                                                id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id VARCHAR(36) NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    service_id VARCHAR(36) NOT NULL REFERENCES services(id) ON DELETE RESTRICT,
    description TEXT,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL
    );

-- Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
                                          id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID, -- Keycloak user ID
    user_role VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    entity VARCHAR(100) NOT NULL,
    entity_id VARCHAR(36),
    description TEXT,
    status VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );