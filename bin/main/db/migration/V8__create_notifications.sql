CREATE TABLE notifications (
    notification_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    receiver_user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(100) NOT NULL,
    message VARCHAR(500) NOT NULL,
    target_type VARCHAR(30) NULL,
    target_id BIGINT NULL,
    link_url VARCHAR(500) NULL,
    is_read BIT NOT NULL DEFAULT 0,
    read_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_notifications_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id),
    CONSTRAINT fk_notifications_receiver
        FOREIGN KEY (receiver_user_id)
        REFERENCES users (id)
);

CREATE INDEX idx_notifications_project_receiver
    ON notifications (project_id, receiver_user_id);

CREATE INDEX idx_notifications_receiver_read
    ON notifications (receiver_user_id, is_read);

CREATE INDEX idx_notifications_deleted_at
    ON notifications (deleted_at);
