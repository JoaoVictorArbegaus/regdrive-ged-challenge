package br.com.regdrive.ged.document.controller;

import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import br.com.regdrive.ged.document.api.DocumentApi;
import br.com.regdrive.ged.document.domain.DocumentStatus;
import br.com.regdrive.ged.document.dto.CreateDocumentRequest;
import br.com.regdrive.ged.document.dto.DocumentListQuery;
import br.com.regdrive.ged.document.dto.DocumentPageResponse;
import br.com.regdrive.ged.document.dto.DocumentResponse;
import br.com.regdrive.ged.document.dto.UpdateDocumentRequest;
import br.com.regdrive.ged.document.dto.UpdateDocumentStatusRequest;
import br.com.regdrive.ged.document.service.DocumentService;
import java.net.URI;
import java.time.Instant;
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

	@Override
	public ResponseEntity<DocumentPageResponse> list(
			Jwt jwt,
			String title,
			String tag,
			DocumentStatus status,
			Instant createdFrom,
			Instant createdTo,
			UUID ownerId,
			String tenantId,
			int page,
			int size,
			String sort) {
		DocumentListQuery query = new DocumentListQuery(
				title, tag, status, createdFrom, createdTo, ownerId, tenantId, page, size, sort);
		return ResponseEntity.ok(documentService.list(query, AuthenticatedUser.from(jwt)));
	}

	@Override
	public ResponseEntity<DocumentResponse> updateMetadata(
			Jwt jwt, UUID documentId, UpdateDocumentRequest request) {
		return ResponseEntity.ok(documentService.updateMetadata(
				documentId, request, AuthenticatedUser.from(jwt)));
	}

	@Override
	public ResponseEntity<DocumentResponse> updateStatus(
			Jwt jwt, UUID documentId, UpdateDocumentStatusRequest request) {
		return ResponseEntity.ok(documentService.updateStatus(
				documentId, request, AuthenticatedUser.from(jwt)));
	}
}
