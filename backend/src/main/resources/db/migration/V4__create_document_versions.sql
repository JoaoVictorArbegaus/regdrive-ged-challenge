CREATE TABLE document_versions (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    file_key VARCHAR(100) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    uploaded_by UUID NOT NULL,
    CONSTRAINT fk_document_versions_document
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_document_versions_user
        FOREIGN KEY (uploaded_by) REFERENCES users (id),
    CONSTRAINT uk_document_versions_number UNIQUE (document_id, version_number),
    CONSTRAINT ck_document_versions_number CHECK (version_number > 0),
    CONSTRAINT ck_document_versions_file_size CHECK (file_size > 0)
);
