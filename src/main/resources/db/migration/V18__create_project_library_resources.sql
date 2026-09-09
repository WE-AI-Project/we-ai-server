CREATE TABLE project_library_resources (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(20) NOT NULL,
    description VARCHAR(1000) NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_content_type VARCHAR(100) NULL,
    extension VARCHAR(20) NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_project_library_resources_project
        FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_library_resources_uploader
        FOREIGN KEY (uploader_id) REFERENCES users (id)
);

CREATE INDEX idx_project_library_resources_project_deleted ON project_library_resources (project_id, deleted_at);
CREATE INDEX idx_project_library_resources_project_category ON project_library_resources (project_id, category, deleted_at);
