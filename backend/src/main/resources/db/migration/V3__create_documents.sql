CREATE TABLE documents (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    owner_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_documents_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_documents_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE TABLE document_tags (
    document_id UUID NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (document_id, tag),
    CONSTRAINT fk_document_tags_document
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

CREATE INDEX idx_documents_tenant_created_at ON documents (tenant_id, created_at);
CREATE INDEX idx_documents_tenant_status ON documents (tenant_id, status);
CREATE INDEX idx_documents_owner_id ON documents (owner_id);
CREATE INDEX idx_document_tags_tag ON document_tags (tag);
