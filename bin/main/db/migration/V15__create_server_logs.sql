CREATE TABLE server_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    level VARCHAR(20) NOT NULL,
    source VARCHAR(30) NOT NULL,
    message VARCHAR(4000) NOT NULL,
    thread_name VARCHAR(255) NULL,
    logger_name VARCHAR(500) NULL,
    trace_id VARCHAR(255) NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_server_logs_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
);

CREATE INDEX idx_server_logs_project_created_at ON server_logs (project_id, created_at);
CREATE INDEX idx_server_logs_project_deleted_at ON server_logs (project_id, deleted_at);
