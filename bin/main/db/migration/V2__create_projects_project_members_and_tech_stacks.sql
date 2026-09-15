CREATE TABLE projects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_name VARCHAR(50) NOT NULL,
    description VARCHAR(500) NULL,
    project_code VARCHAR(20) NOT NULL,
    local_path VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL,
    start_date DATE NULL,
    target_date DATE NULL,
    created_by BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT idx_projects_project_code UNIQUE (project_code),
    CONSTRAINT fk_projects_created_by
        FOREIGN KEY (created_by)
        REFERENCES users (id)
);

CREATE TABLE project_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    department VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT idx_project_members_project_user UNIQUE (project_id, user_id),
    CONSTRAINT fk_project_members_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id),
    CONSTRAINT fk_project_members_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
);

CREATE TABLE project_tech_stacks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    version VARCHAR(30) NULL,
    category VARCHAR(30) NOT NULL,
    is_required BIT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_project_tech_stacks_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id)
);
