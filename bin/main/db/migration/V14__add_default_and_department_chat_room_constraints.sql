ALTER TABLE chat_rooms
    ADD COLUMN is_default BIT NOT NULL DEFAULT 0 AFTER is_private,
    ADD COLUMN active_default_project_id BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN is_default = 1 AND status = 'ACTIVE' AND deleted_at IS NULL THEN project_id
                ELSE NULL
            END
        ) STORED,
    ADD COLUMN active_department_room_key VARCHAR(80)
        GENERATED ALWAYS AS (
            CASE
                WHEN type = 'DEPARTMENT' AND status = 'ACTIVE' AND deleted_at IS NULL
                    THEN CONCAT(project_id, ':', department)
                ELSE NULL
            END
        ) STORED,
    ADD CONSTRAINT uk_chat_rooms_active_default_project UNIQUE (active_default_project_id),
    ADD CONSTRAINT uk_chat_rooms_active_department UNIQUE (active_department_room_key);
