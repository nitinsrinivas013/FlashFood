-- V5: Create donation_claims table
CREATE TABLE donation_claims (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donation_id BIGINT NOT NULL,
    ngo_id BIGINT NOT NULL,
    accepted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pickup_status ENUM('ASSIGNED', 'EN_ROUTE', 'ARRIVED', 'COLLECTED', 'COMPLETED') NOT NULL DEFAULT 'ASSIGNED',
    collected_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    completion_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_donation_claims_donation_id UNIQUE (donation_id),
    CONSTRAINT fk_donation_claims_donation FOREIGN KEY (donation_id) REFERENCES donations(id) ON DELETE CASCADE,
    CONSTRAINT fk_donation_claims_ngo FOREIGN KEY (ngo_id) REFERENCES ngo_profiles(id) ON DELETE CASCADE,
    INDEX idx_donation_claims_donation_id (donation_id),
    INDEX idx_donation_claims_ngo_id (ngo_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
