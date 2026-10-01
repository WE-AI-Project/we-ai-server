CREATE TABLE rag_documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    source VARCHAR(500) NOT NULL,
    origin VARCHAR(30) NOT NULL,
    chunk_count INT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding_model VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rag_documents_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE UNIQUE INDEX uk_rag_documents_project_source ON rag_documents (project_id, source);

CREATE INDEX idx_rag_documents_project_origin ON rag_documents (project_id, origin);
