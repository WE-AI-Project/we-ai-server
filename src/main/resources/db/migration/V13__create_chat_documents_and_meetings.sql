CREATE TABLE chat_documents (
    document_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_content_type VARCHAR(100) NULL,
    extension VARCHAR(20) NOT NULL,
    description VARCHAR(500) NULL,
    extracted_text LONGTEXT NULL,
    status VARCHAR(30) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (document_id),
    CONSTRAINT fk_chat_documents_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id),
    CONSTRAINT fk_chat_documents_uploader
        FOREIGN KEY (uploader_id)
        REFERENCES users (id)
);

CREATE INDEX idx_chat_documents_project_status
    ON chat_documents (project_id, status);

CREATE INDEX idx_chat_documents_project_created
    ON chat_documents (project_id, created_at);

CREATE TABLE document_briefings (
    briefing_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    document_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,
    summary VARCHAR(2000) NOT NULL,
    key_points LONGTEXT NULL,
    action_items LONGTEXT NULL,
    risks LONGTEXT NULL,
    keywords LONGTEXT NULL,
    status VARCHAR(30) NOT NULL,
    PRIMARY KEY (briefing_id),
    CONSTRAINT fk_document_briefings_document
        FOREIGN KEY (document_id)
        REFERENCES chat_documents (document_id),
    CONSTRAINT fk_document_briefings_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id),
    CONSTRAINT fk_document_briefings_creator
        FOREIGN KEY (creator_id)
        REFERENCES users (id)
);

CREATE INDEX idx_document_briefings_project_status
    ON document_briefings (project_id, status);

CREATE INDEX idx_document_briefings_document_created
    ON document_briefings (document_id, created_at);

CREATE TABLE chat_meetings (
    meeting_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    chat_room_id BIGINT NULL,
    host_user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    ended_at DATETIME(6) NULL,
    PRIMARY KEY (meeting_id),
    CONSTRAINT fk_chat_meetings_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id),
    CONSTRAINT fk_chat_meetings_room
        FOREIGN KEY (chat_room_id)
        REFERENCES chat_rooms (chat_room_id),
    CONSTRAINT fk_chat_meetings_host
        FOREIGN KEY (host_user_id)
        REFERENCES users (id)
);

CREATE INDEX idx_chat_meetings_project_status
    ON chat_meetings (project_id, status);

CREATE INDEX idx_chat_meetings_project_started
    ON chat_meetings (project_id, started_at);

CREATE TABLE meeting_minutes (
    minute_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    meeting_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    writer_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content LONGTEXT NOT NULL,
    summary VARCHAR(2000) NULL,
    action_items LONGTEXT NULL,
    status VARCHAR(30) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (minute_id),
    CONSTRAINT fk_meeting_minutes_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES chat_meetings (meeting_id),
    CONSTRAINT fk_meeting_minutes_project
        FOREIGN KEY (project_id)
        REFERENCES projects (id),
    CONSTRAINT fk_meeting_minutes_writer
        FOREIGN KEY (writer_id)
        REFERENCES users (id)
);

CREATE INDEX idx_meeting_minutes_project_created
    ON meeting_minutes (project_id, created_at);

CREATE INDEX idx_meeting_minutes_meeting
    ON meeting_minutes (meeting_id);

CREATE TABLE meeting_participants (
    meeting_participant_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    meeting_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    PRIMARY KEY (meeting_participant_id),
    CONSTRAINT fk_meeting_participants_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES chat_meetings (meeting_id),
    CONSTRAINT fk_meeting_participants_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
);

CREATE INDEX idx_meeting_participants_meeting
    ON meeting_participants (meeting_id);

CREATE INDEX idx_meeting_participants_user
    ON meeting_participants (user_id);
