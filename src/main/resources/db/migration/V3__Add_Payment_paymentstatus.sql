-- Payments table
-- Payment Statuses lookup table
CREATE TABLE IF NOT EXISTS payment_statuses (
                                                id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
                                                name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS payments (
                                        id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
                                        reservation_id VARCHAR(36) NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
                                        status_id VARCHAR(36) NOT NULL REFERENCES payment_statuses(id) ON DELETE RESTRICT,
                                        amount DECIMAL(10,2) NOT NULL,
                                        method VARCHAR(50) NOT NULL,
                                        transaction_code VARCHAR(100),
                                        payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


INSERT INTO payment_statuses (name) VALUES
                                        ('PENDING'),
                                        ('COMPLETED'),
                                        ('FAILED'),
                                        ('REFUNDED'),
                                        ('PROCESSING')
ON CONFLICT (name) DO NOTHING;