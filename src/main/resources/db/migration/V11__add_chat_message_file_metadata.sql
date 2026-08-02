ALTER TABLE chat_messages
    MODIFY content VARCHAR(2000) NULL;

ALTER TABLE chat_messages
    ADD COLUMN file_url VARCHAR(500) NULL,
    ADD COLUMN original_file_name VARCHAR(255) NULL,
    ADD COLUMN stored_file_name VARCHAR(255) NULL,
    ADD COLUMN file_size BIGINT NULL,
    ADD COLUMN file_content_type VARCHAR(100) NULL;
