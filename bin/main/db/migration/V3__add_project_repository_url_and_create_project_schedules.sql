ALTER TABLE projects
    ADD COLUMN repository_url VARCHAR(500) NULL;

CREATE TABLE project_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    assignee_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NULL,
    department VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_project_schedules_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id),
    CONSTRAINT fk_project_schedules_assignee
        FOREIGN KEY (assignee_id)
        REFERENCES users (id)
);

CREATE INDEX idx_project_schedules_project_start_date
    ON project_schedules (project_id, start_date);

CREATE INDEX idx_project_schedules_project_status
    ON project_schedules (project_id, status);
