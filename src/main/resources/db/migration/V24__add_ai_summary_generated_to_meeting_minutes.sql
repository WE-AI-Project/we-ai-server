ALTER TABLE meeting_minutes
    ADD COLUMN ai_summary_generated BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'True when summary was produced by the AI meeting summary model; false when it is a truncated-content fallback or a user-supplied summary.';
