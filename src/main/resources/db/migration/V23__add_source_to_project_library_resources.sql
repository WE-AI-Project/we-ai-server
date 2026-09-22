ALTER TABLE project_library_resources
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN source_document_id BIGINT NULL;

CREATE INDEX idx_project_library_resources_source_document ON project_library_resources (source_document_id);
