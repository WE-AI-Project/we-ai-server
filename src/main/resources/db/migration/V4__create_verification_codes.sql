CREATE TABLE verification_codes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    email VARCHAR(100) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    delivery_channel VARCHAR(20) NOT NULL,
    delivery_target VARCHAR(255) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_verification_codes_email_purpose_used_created
    ON verification_codes (email, purpose, used_at, created_at);
