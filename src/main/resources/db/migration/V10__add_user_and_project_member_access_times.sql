ALTER TABLE users
    ADD COLUMN last_login_at DATETIME(6) NULL,
    ADD COLUMN last_accessed_at DATETIME(6) NULL;

ALTER TABLE project_members
    ADD COLUMN last_accessed_at DATETIME(6) NULL;
