CREATE TABLE project_milestones (
    milestone_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    progress_rate INT NOT NULL,
    PRIMARY KEY (milestone_id),
    CONSTRAINT fk_project_milestones_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
);

CREATE INDEX idx_project_milestones_project_start_date
    ON project_milestones (project_id, start_date);

CREATE INDEX idx_project_milestones_project_status
    ON project_milestones (project_id, status);
