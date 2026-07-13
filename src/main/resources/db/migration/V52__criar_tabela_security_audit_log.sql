CREATE TABLE security_audit_log (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    username VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    action VARCHAR(20) NOT NULL,
    resource VARCHAR(255) NOT NULL,
    details TEXT
);

CREATE INDEX idx_security_audit_username ON security_audit_log(username);
CREATE INDEX idx_security_audit_timestamp ON security_audit_log(timestamp);
