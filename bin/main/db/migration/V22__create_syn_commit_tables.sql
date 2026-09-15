CREATE TABLE syn_pending_changes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    registered_by BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    diff_content LONGTEXT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_syn_pending_changes_project
        FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_syn_pending_changes_registered_by
        FOREIGN KEY (registered_by) REFERENCES users (id)
);

CREATE INDEX idx_syn_pending_changes_project ON syn_pending_changes (project_id);

CREATE UNIQUE INDEX idx_syn_pending_changes_project_path ON syn_pending_changes (project_id, file_path);

CREATE TABLE syn_commits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    commit_message VARCHAR(500) NOT NULL,
    summary LONGTEXT NOT NULL,
    changed_file_count INT NOT NULL,
    committed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_syn_commits_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE INDEX idx_syn_commits_project_committed ON syn_commits (project_id, committed_at);

CREATE TABLE syn_commit_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    syn_commit_id BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    diff_content LONGTEXT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_syn_commit_files_commit
        FOREIGN KEY (syn_commit_id) REFERENCES syn_commits (id)
);

CREATE INDEX idx_syn_commit_files_commit ON syn_commit_files (syn_commit_id);
