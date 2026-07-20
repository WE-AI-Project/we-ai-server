CREATE TABLE chat_rooms (
    chat_room_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    type VARCHAR(30) NOT NULL,
    department VARCHAR(30) NULL,
    status VARCHAR(20) NOT NULL,
    is_private BIT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (chat_room_id),
    CONSTRAINT fk_chat_rooms_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id),
    CONSTRAINT fk_chat_rooms_created_by
        FOREIGN KEY (created_by)
        REFERENCES users (id)
);

CREATE TABLE chat_messages (
    chat_message_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    chat_room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    message_type VARCHAR(30) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (chat_message_id),
    CONSTRAINT fk_chat_messages_room
        FOREIGN KEY (chat_room_id)
        REFERENCES chat_rooms (chat_room_id),
    CONSTRAINT fk_chat_messages_sender
        FOREIGN KEY (sender_id)
        REFERENCES users (id)
);

CREATE TABLE chat_room_members (
    chat_room_member_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    chat_room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_read_message_id BIGINT NULL,
    joined_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (chat_room_member_id),
    CONSTRAINT idx_chat_room_members_room_user UNIQUE (chat_room_id, user_id),
    CONSTRAINT fk_chat_room_members_room
        FOREIGN KEY (chat_room_id)
        REFERENCES chat_rooms (chat_room_id),
    CONSTRAINT fk_chat_room_members_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),
    CONSTRAINT fk_chat_room_members_last_read_message
        FOREIGN KEY (last_read_message_id)
        REFERENCES chat_messages (chat_message_id)
);

CREATE INDEX idx_chat_rooms_project_status
    ON chat_rooms (project_id, status);

CREATE INDEX idx_chat_rooms_project_type
    ON chat_rooms (project_id, type);

CREATE INDEX idx_chat_rooms_project_department
    ON chat_rooms (project_id, department);

CREATE INDEX idx_chat_messages_room_created
    ON chat_messages (chat_room_id, created_at);

CREATE INDEX idx_chat_messages_room_id
    ON chat_messages (chat_room_id, chat_message_id);

CREATE INDEX idx_chat_room_members_user_status
    ON chat_room_members (user_id, status);
