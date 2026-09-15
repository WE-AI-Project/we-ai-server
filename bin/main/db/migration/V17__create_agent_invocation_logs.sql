CREATE TABLE agent_invocation_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    agent VARCHAR(20) NOT NULL,
    project_id BIGINT NOT NULL,
    success BOOLEAN NOT NULL,
    duration_ms BIGINT NOT NULL,
    error_message VARCHAR(2000) NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_agent_invocation_logs_agent_created_at ON agent_invocation_logs (agent, created_at);
