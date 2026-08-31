package br.com.regdrive.ged.document.service;

import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import br.com.regdrive.ged.document.dto.CreateDocumentRequest;
import br.com.regdrive.ged.document.dto.DocumentResponse;
import java.util.UUID;

public interface DocumentService {

	DocumentResponse create(CreateDocumentRequest request, AuthenticatedUser authenticatedUser);

	DocumentResponse findById(UUID documentId, AuthenticatedUser authenticatedUser);
}
