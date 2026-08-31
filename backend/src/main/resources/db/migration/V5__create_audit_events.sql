CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    user_id UUID NOT NULL,
    action VARCHAR(30) NOT NULL,
    metadata JSONB NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_audit_events_document
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_events_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_audit_events_action CHECK (action IN (
        'DOCUMENT_CREATED',
        'DOCUMENT_UPDATED',
        'DOCUMENT_PUBLISHED',
        'DOCUMENT_ARCHIVED',
        'FILE_UPLOADED',
        'FILE_DOWNLOADED'
    ))
);

CREATE INDEX idx_audit_events_document_occurred_at
    ON audit_events (document_id, occurred_at);
