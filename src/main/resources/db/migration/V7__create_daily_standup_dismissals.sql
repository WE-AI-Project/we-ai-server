CREATE TABLE daily_standup_dismissals (
    dismissal_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    dismiss_date DATE NOT NULL,
    dismissed_until DATETIME(6) NOT NULL,
    PRIMARY KEY (dismissal_id),
    CONSTRAINT idx_daily_standup_dismissals_project_user_date UNIQUE (project_id, user_id, dismiss_date),
    CONSTRAINT fk_daily_standup_dismissals_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id),
    CONSTRAINT fk_daily_standup_dismissals_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
);
