CREATE TABLE user_ai_backend_settings (
    user_ai_backend_setting_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    dialect VARCHAR(30) NOT NULL DEFAULT 'OLLAMA_NATIVE',
    base_url VARCHAR(500) NULL,
    model_name VARCHAR(100) NULL,
    health_path VARCHAR(200) NOT NULL DEFAULT '/api/tags',
    encrypted_api_key TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_user_ai_backend_settings_user UNIQUE (user_id),
    CONSTRAINT fk_user_ai_backend_settings_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE project_ai_backend_settings (
    project_ai_backend_setting_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    dialect VARCHAR(30) NOT NULL DEFAULT 'OLLAMA_NATIVE',
    base_url VARCHAR(500) NULL,
    model_name VARCHAR(100) NULL,
    health_path VARCHAR(200) NOT NULL DEFAULT '/api/tags',
    encrypted_api_key TEXT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_project_ai_backend_settings_project UNIQUE (project_id),
    CONSTRAINT fk_project_ai_backend_settings_project
        FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_ai_backend_settings_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_project_ai_backend_settings_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id)
);
