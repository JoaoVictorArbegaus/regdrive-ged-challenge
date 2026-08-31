package br.com.regdrive.ged.document.api;

import br.com.regdrive.ged.document.domain.DocumentStatus;
import br.com.regdrive.ged.document.dto.CreateDocumentRequest;
import br.com.regdrive.ged.document.dto.DocumentPageResponse;
import br.com.regdrive.ged.document.dto.DocumentResponse;
import br.com.regdrive.ged.document.dto.UpdateDocumentRequest;
import br.com.regdrive.ged.document.dto.UpdateDocumentStatusRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/documents")
public interface DocumentApi {

	@PostMapping
	ResponseEntity<DocumentResponse> create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateDocumentRequest request);

	@GetMapping("/{documentId}")
	ResponseEntity<DocumentResponse> findById(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID documentId);

	@GetMapping
	ResponseEntity<DocumentPageResponse> list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) String title,
			@RequestParam(required = false) String tag,
			@RequestParam(required = false) DocumentStatus status,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
			@RequestParam(required = false) UUID ownerId,
			@RequestParam(required = false) String tenantId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort);

	@PutMapping("/{documentId}")
	ResponseEntity<DocumentResponse> updateMetadata(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID documentId,
			@Valid @RequestBody UpdateDocumentRequest request);

	@PatchMapping("/{documentId}/status")
	ResponseEntity<DocumentResponse> updateStatus(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID documentId,
			@Valid @RequestBody UpdateDocumentStatusRequest request);
}
