-- V4: Create donations table
CREATE TABLE donations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    food_name VARCHAR(255) NOT NULL,
    description TEXT,
    food_category ENUM('COOKED_MEALS', 'RAW_INGREDIENTS', 'BAKERY', 'DAIRY', 'FRUITS_VEGETABLES', 'BEVERAGES', 'PACKAGED', 'OTHER') NOT NULL DEFAULT 'OTHER',
    quantity DECIMAL(10, 2) NOT NULL,
    quantity_unit ENUM('KG', 'PORTIONS', 'PACKETS', 'LITERS', 'PIECES') NOT NULL,
    image_url VARCHAR(512),
    prepared_at TIMESTAMP NULL,
    available_until TIMESTAMP NOT NULL,
    pickup_deadline TIMESTAMP NOT NULL,
    storage_instructions TEXT,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    address TEXT NOT NULL,
    status ENUM('AVAILABLE', 'ACCEPTED', 'COLLECTED', 'COMPLETED', 'EXPIRED', 'CANCELLED') NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_donations_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurant_profiles(id) ON DELETE CASCADE,
    INDEX idx_donations_restaurant_id (restaurant_id),
    INDEX idx_donations_status (status),
    INDEX idx_donations_pickup_deadline (pickup_deadline),
    INDEX idx_donations_location (latitude, longitude),
    INDEX idx_donations_status_deadline (status, pickup_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
