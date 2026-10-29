CREATE TABLE ssl_certificate_monitors (
    id VARCHAR(36) PRIMARY KEY,
    url VARCHAR(2048) NOT NULL,
    check_time VARCHAR(5) NOT NULL,
    alert_threshold_days INTEGER NOT NULL,
    alert_weekdays VARCHAR(40) NOT NULL,
    notification_channel VARCHAR(20) NOT NULL,
    notification_target VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(100) NOT NULL,
    last_checked_at TIMESTAMPTZ,
    last_check_local_date DATE,
    last_alert_local_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ssl_monitors_active ON ssl_certificate_monitors(active, check_time);

CREATE TABLE ssl_certificate_check_history (
    id VARCHAR(36) PRIMARY KEY,
    monitor_id VARCHAR(36) NOT NULL REFERENCES ssl_certificate_monitors(id) ON DELETE CASCADE,
    checked_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL,
    days_remaining INTEGER,
    expires_at TIMESTAMPTZ,
    checked_url VARCHAR(2048),
    error_message TEXT,
    alert_sent BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_ssl_history_monitor_checked ON ssl_certificate_check_history(monitor_id, checked_at DESC);
