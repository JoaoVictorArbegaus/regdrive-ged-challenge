package br.com.regdrive.ged.document.controller;

import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import br.com.regdrive.ged.document.api.DocumentApi;
import br.com.regdrive.ged.document.dto.CreateDocumentRequest;
import br.com.regdrive.ged.document.dto.DocumentResponse;
import br.com.regdrive.ged.document.service.DocumentService;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DocumentController implements DocumentApi {

	private final DocumentService documentService;

	@Override
	public ResponseEntity<DocumentResponse> create(Jwt jwt, CreateDocumentRequest request) {
		DocumentResponse response = documentService.create(request, AuthenticatedUser.from(jwt));
		return ResponseEntity.created(URI.create("/api/documents/" + response.id())).body(response);
	}

	@Override
	public ResponseEntity<DocumentResponse> findById(Jwt jwt, UUID documentId) {
		return ResponseEntity.ok(documentService.findById(documentId, AuthenticatedUser.from(jwt)));
	}
}
