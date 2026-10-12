CREATE TABLE IF NOT EXISTS app_setting (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO app_setting (setting_key, setting_value, description)
VALUES ('app.initialized', 'true', 'Created by the initial Flyway migration')
ON CONFLICT (setting_key) DO NOTHING;
