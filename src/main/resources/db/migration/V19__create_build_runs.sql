CREATE TABLE build_runs (
    build_run_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    task_name VARCHAR(50) NOT NULL,
    build_tool VARCHAR(20) NOT NULL,
    profile VARCHAR(30) NULL,
    status VARCHAR(20) NOT NULL,
    command VARCHAR(500) NOT NULL,
    exit_code INT NULL,
    output TEXT NULL,
    error_output TEXT NULL,
    started_at DATETIME(6) NULL,
    finished_at DATETIME(6) NULL,
    duration_ms BIGINT NULL,
    PRIMARY KEY (build_run_id),
    CONSTRAINT fk_build_runs_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id),
    CONSTRAINT fk_build_runs_requester
        FOREIGN KEY (requester_id)
        REFERENCES users (id)
);

CREATE INDEX idx_build_runs_project_created_at ON build_runs (project_id, created_at);
CREATE INDEX idx_build_runs_project_status ON build_runs (project_id, status);
