package br.com.regdrive.ged.document.controller;

import br.com.regdrive.ged.audit.dto.AuditEventResponse;
import br.com.regdrive.ged.audit.service.AuditService;
import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import br.com.regdrive.ged.document.api.DocumentApi;
import br.com.regdrive.ged.document.domain.DocumentStatus;
import br.com.regdrive.ged.document.dto.CreateDocumentRequest;
import br.com.regdrive.ged.document.dto.DocumentListQuery;
import br.com.regdrive.ged.document.dto.DocumentPageResponse;
import br.com.regdrive.ged.document.dto.DocumentResponse;
import br.com.regdrive.ged.document.dto.DocumentVersionDownload;
import br.com.regdrive.ged.document.dto.DocumentVersionResponse;
import br.com.regdrive.ged.document.dto.UpdateDocumentRequest;
import br.com.regdrive.ged.document.dto.UpdateDocumentStatusRequest;
import br.com.regdrive.ged.document.service.DocumentService;
import br.com.regdrive.ged.document.service.DocumentVersionService;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class DocumentController implements DocumentApi {

	private final DocumentService documentService;
	private final DocumentVersionService documentVersionService;
	private final AuditService auditService;

	@Override
	public ResponseEntity<DocumentResponse> create(Jwt jwt, CreateDocumentRequest request) {
		DocumentResponse response = documentService.create(request, AuthenticatedUser.from(jwt));
		return ResponseEntity.created(URI.create("/api/documents/" + response.id())).body(response);
	}

	@Override
	public ResponseEntity<DocumentVersionResponse> uploadVersion(
			Jwt jwt, UUID documentId, MultipartFile file) {
		DocumentVersionResponse response = documentVersionService.upload(
				documentId, file, AuthenticatedUser.from(jwt));
		URI location = URI.create(
				"/api/documents/" + documentId + "/versions/" + response.versionNumber());
		return ResponseEntity.created(location).body(response);
	}

	@Override
	public ResponseEntity<List<DocumentVersionResponse>> listVersions(Jwt jwt, UUID documentId) {
		return ResponseEntity.ok(documentVersionService.list(
				documentId, AuthenticatedUser.from(jwt)));
	}

	@Override
	public ResponseEntity<DocumentVersionResponse> findVersion(
			Jwt jwt, UUID documentId, int versionNumber) {
		return ResponseEntity.ok(documentVersionService.findByVersionNumber(
				documentId, versionNumber, AuthenticatedUser.from(jwt)));
	}

	@Override
	public ResponseEntity<byte[]> downloadVersion(
			Jwt jwt, UUID documentId, int versionNumber) {
		DocumentVersionDownload download = documentVersionService.download(
				documentId, versionNumber, AuthenticatedUser.from(jwt));
		ContentDisposition disposition = ContentDisposition.attachment()
				.filename(download.filename(), StandardCharsets.UTF_8)
				.build();
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(download.mimeType()))
				.contentLength(download.fileSize())
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
				.body(download.content());
	}

	@Override
	public ResponseEntity<List<AuditEventResponse>> listAudit(Jwt jwt, UUID documentId) {
		return ResponseEntity.ok(auditService.list(documentId, AuthenticatedUser.from(jwt)));
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
