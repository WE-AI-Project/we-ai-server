CREATE TABLE qa_runs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    commit_id VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL,
    progress_rate INT NOT NULL,
    current_step INT NULL,
    total_step INT NULL,
    started_at DATETIME(6) NULL,
    finished_at DATETIME(6) NULL,
    error_message VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_qa_runs_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
);

CREATE INDEX idx_qa_runs_project_status
    ON qa_runs (project_id, status);

CREATE INDEX idx_qa_runs_project_commit
    ON qa_runs (project_id, commit_id);

CREATE TABLE qa_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    qa_run_id BIGINT NULL,
    commit_id VARCHAR(100) NULL,
    commit_hash VARCHAR(100) NULL,
    commit_message VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL,
    summary VARCHAR(2000) NULL,
    total_issue_count INT NOT NULL,
    critical_count INT NOT NULL,
    major_count INT NOT NULL,
    minor_count INT NOT NULL,
    test_pass_count INT NOT NULL,
    test_fail_count INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_qa_reports_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id),
    CONSTRAINT fk_qa_reports_run
        FOREIGN KEY (qa_run_id)
        REFERENCES qa_runs (id)
);

CREATE INDEX idx_qa_reports_project_created
    ON qa_reports (project_id, created_at);

CREATE INDEX idx_qa_reports_project_commit
    ON qa_reports (project_id, commit_id);

CREATE INDEX idx_qa_reports_project_status
    ON qa_reports (project_id, status);

CREATE TABLE qa_report_issues (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    qa_report_id BIGINT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NULL,
    file_path VARCHAR(500) NULL,
    line_number INT NULL,
    suggestion VARCHAR(2000) NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_qa_report_issues_report
        FOREIGN KEY (qa_report_id)
        REFERENCES qa_reports (id)
);

CREATE INDEX idx_qa_report_issues_report
    ON qa_report_issues (qa_report_id);

CREATE TABLE qa_report_test_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    qa_report_id BIGINT NOT NULL,
    test_name VARCHAR(200) NOT NULL,
    test_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    message VARCHAR(1000) NULL,
    duration_ms BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_qa_report_test_results_report
        FOREIGN KEY (qa_report_id)
        REFERENCES qa_reports (id)
);

CREATE INDEX idx_qa_report_test_results_report
    ON qa_report_test_results (qa_report_id);
