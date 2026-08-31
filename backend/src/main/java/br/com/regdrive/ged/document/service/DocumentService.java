package br.com.regdrive.ged.document.service;

import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import br.com.regdrive.ged.document.dto.CreateDocumentRequest;
import br.com.regdrive.ged.document.dto.DocumentResponse;
import br.com.regdrive.ged.document.dto.DocumentListQuery;
import br.com.regdrive.ged.document.dto.DocumentPageResponse;
import br.com.regdrive.ged.document.dto.UpdateDocumentRequest;
import br.com.regdrive.ged.document.dto.UpdateDocumentStatusRequest;
import java.util.UUID;

public interface DocumentService {

	DocumentResponse create(CreateDocumentRequest request, AuthenticatedUser authenticatedUser);

	DocumentResponse findById(UUID documentId, AuthenticatedUser authenticatedUser);

	DocumentPageResponse list(DocumentListQuery query, AuthenticatedUser authenticatedUser);

	DocumentResponse updateMetadata(
			UUID documentId, UpdateDocumentRequest request, AuthenticatedUser authenticatedUser);

	DocumentResponse updateStatus(
			UUID documentId, UpdateDocumentStatusRequest request, AuthenticatedUser authenticatedUser);
}
