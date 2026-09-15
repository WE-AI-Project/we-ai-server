CREATE TABLE project_environment_settings (
    environment_setting_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    active_profile VARCHAR(30) NOT NULL,
    build_tool VARCHAR(30) NOT NULL DEFAULT 'GRADLE',
    java_version VARCHAR(50) NULL,
    changed_by BIGINT NULL,
    changed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_project_environment_settings_project UNIQUE (project_id),
    CONSTRAINT fk_project_environment_settings_project
        FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_environment_settings_changed_by
        FOREIGN KEY (changed_by) REFERENCES users (id)
);

CREATE INDEX idx_project_environment_settings_project
    ON project_environment_settings (project_id);
