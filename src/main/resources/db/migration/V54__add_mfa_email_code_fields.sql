ALTER TABLE usuarios ADD COLUMN mfa_code VARCHAR(6);
ALTER TABLE usuarios ADD COLUMN mfa_code_expires_at TIMESTAMP;
