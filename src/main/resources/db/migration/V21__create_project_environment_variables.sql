CREATE TABLE project_environment_variables (
    environment_variable_id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    profile VARCHAR(30) NOT NULL,
    variable_key VARCHAR(100) NOT NULL,
    encrypted_value TEXT NOT NULL,
    secret BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(500) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted_at DATETIME(6) NULL,
    active_key VARCHAR(131) GENERATED ALWAYS AS (
        CASE WHEN deleted_at IS NULL THEN CONCAT(profile, ':', variable_key) ELSE NULL END
    ) STORED,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (environment_variable_id),
    CONSTRAINT fk_project_environment_variables_project
        FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_environment_variables_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_project_environment_variables_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id)
);

CREATE UNIQUE INDEX uk_project_environment_variables_active_key
    ON project_environment_variables (project_id, active_key);

CREATE INDEX idx_project_environment_variables_project_profile
    ON project_environment_variables (project_id, profile);

CREATE INDEX idx_project_environment_variables_active
    ON project_environment_variables (project_id, profile, enabled, deleted_at);
