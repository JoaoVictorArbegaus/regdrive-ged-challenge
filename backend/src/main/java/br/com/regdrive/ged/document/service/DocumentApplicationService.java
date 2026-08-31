package br.com.regdrive.ged.document.service;

import br.com.regdrive.ged.audit.domain.AuditAction;
import br.com.regdrive.ged.audit.service.AuditService;
import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import br.com.regdrive.ged.document.domain.Document;
import br.com.regdrive.ged.document.domain.DocumentStatus;
import br.com.regdrive.ged.document.dto.CreateDocumentRequest;
import br.com.regdrive.ged.document.dto.DocumentListQuery;
import br.com.regdrive.ged.document.dto.DocumentPageResponse;
import br.com.regdrive.ged.document.dto.DocumentResponse;
import br.com.regdrive.ged.document.dto.UpdateDocumentRequest;
import br.com.regdrive.ged.document.dto.UpdateDocumentStatusRequest;
import br.com.regdrive.ged.document.exception.DocumentNotFoundException;
import br.com.regdrive.ged.document.exception.InvalidDocumentListParameterException;
import br.com.regdrive.ged.document.exception.OwnerNotFoundException;
import br.com.regdrive.ged.document.repository.DocumentRepository;
import br.com.regdrive.ged.user.domain.Role;
import br.com.regdrive.ged.user.repository.UserAccountRepository;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DocumentApplicationService implements DocumentService {

	private static final Set<String> ALLOWED_SORT_PROPERTIES =
			Set.of("title", "status", "createdAt", "updatedAt");

	private final DocumentRepository documentRepository;
	private final UserAccountRepository userRepository;
	private final AuditService auditService;

	@Override
	@Transactional
	public DocumentResponse create(CreateDocumentRequest request, AuthenticatedUser authenticatedUser) {
		ensureCanWrite(authenticatedUser);

		String tenantId = authenticatedUser.tenantId();
		UUID ownerId = authenticatedUser.userId();
		if (authenticatedUser.isAdmin()) {
			if (request.tenantId() != null && !request.tenantId().isBlank()) {
				tenantId = request.tenantId().trim();
			}
			if (request.ownerId() != null) {
				ownerId = request.ownerId();
			}
		}

		if (!userRepository.existsById(ownerId)) {
			throw new OwnerNotFoundException();
		}

		Document document = new Document(
				request.title(),
				request.description(),
				request.tags(),
				tenantId,
				ownerId);
		Document savedDocument = documentRepository.save(document);
		auditService.record(
				savedDocument.getId(),
				authenticatedUser,
				AuditAction.DOCUMENT_CREATED,
				Map.of(
						"status", savedDocument.getStatus().name(),
						"tenantId", savedDocument.getTenantId()));
		return toResponse(savedDocument);
	}

	@Override
	public DocumentResponse findById(UUID documentId, AuthenticatedUser authenticatedUser) {
		return toResponse(findAccessibleDocument(documentId, authenticatedUser));
	}

	@Override
	public DocumentPageResponse list(DocumentListQuery query, AuthenticatedUser authenticatedUser) {
		validateListQuery(query);
		String requestedTenant = normalize(query.tenantId());
		String effectiveTenant;
		if (authenticatedUser.isAdmin()) {
			effectiveTenant = requestedTenant;
		} else {
			effectiveTenant = authenticatedUser.tenantId();
		}
		String title = normalize(query.title());
		String titlePattern;
		if (title == null) {
			titlePattern = null;
		} else {
			titlePattern = "%" + title.toLowerCase(Locale.ROOT) + "%";
		}
		Pageable pageable = createPageable(query);

		Page<Document> documents = documentRepository.search(
				effectiveTenant,
				query.ownerId(),
				query.status(),
				titlePattern,
				normalize(query.tag()),
				query.createdFrom(),
				query.createdTo(),
				pageable);
		return new DocumentPageResponse(
				documents.getContent().stream().map(this::toResponse).toList(),
				documents.getNumber(),
				documents.getSize(),
				documents.getTotalElements(),
				documents.getTotalPages());
	}

	@Override
	@Transactional
	public DocumentResponse updateMetadata(
			UUID documentId, UpdateDocumentRequest request, AuthenticatedUser authenticatedUser) {
		ensureCanWrite(authenticatedUser);
		Document document = findAccessibleDocument(documentId, authenticatedUser);
		document.updateMetadata(request.title(), request.description(), request.tags());
		auditService.record(
				documentId,
				authenticatedUser,
				AuditAction.DOCUMENT_UPDATED,
				Map.of("changedFields", Set.of("title", "description", "tags")));
		return toResponse(document);
	}

	@Override
	@Transactional
	public DocumentResponse updateStatus(
			UUID documentId, UpdateDocumentStatusRequest request, AuthenticatedUser authenticatedUser) {
		ensureCanWrite(authenticatedUser);
		Document document = findAccessibleDocument(documentId, authenticatedUser);
		DocumentStatus previousStatus = document.getStatus();
		document.transitionTo(request.status());
		AuditAction action;
		if (request.status() == DocumentStatus.PUBLISHED) {
			action = AuditAction.DOCUMENT_PUBLISHED;
		} else {
			action = AuditAction.DOCUMENT_ARCHIVED;
		}
		auditService.record(
				documentId,
				authenticatedUser,
				action,
				Map.of(
						"previousStatus", previousStatus.name(),
						"newStatus", document.getStatus().name()));
		return toResponse(document);
	}

	private void ensureCanWrite(AuthenticatedUser authenticatedUser) {
		if (authenticatedUser.role() == Role.VIEWER) {
			throw new AccessDeniedException("Usuário sem permissão para alterar documentos.");
		}
	}

	private Document findAccessibleDocument(UUID documentId, AuthenticatedUser authenticatedUser) {
		if (authenticatedUser.isAdmin()) {
			return documentRepository.findById(documentId).orElseThrow(DocumentNotFoundException::new);
		} else {
			return documentRepository.findByIdAndTenantId(documentId, authenticatedUser.tenantId())
					.orElseThrow(DocumentNotFoundException::new);
		}
	}

	private void validateListQuery(DocumentListQuery query) {
		if (query.page() < 0) {
			throw new InvalidDocumentListParameterException("page", "A página não pode ser negativa.");
		}
		if (query.size() < 1 || query.size() > 100) {
			throw new InvalidDocumentListParameterException(
					"size", "O tamanho da página deve estar entre 1 e 100.");
		}
		if (query.createdFrom() != null
				&& query.createdTo() != null
				&& query.createdFrom().isAfter(query.createdTo())) {
			throw new InvalidDocumentListParameterException(
					"createdFrom", "O período de criação informado é inválido.");
		}
	}

	private Pageable createPageable(DocumentListQuery query) {
		String sortValue = normalize(query.sort());
		if (sortValue == null) {
			sortValue = "createdAt,desc";
		}
		String[] parts = sortValue.split(",", -1);
		if (parts.length != 2 || !ALLOWED_SORT_PROPERTIES.contains(parts[0].trim())) {
			throw new InvalidDocumentListParameterException("sort", "A ordenação informada é inválida.");
		}

		Sort.Direction direction;
		try {
			direction = Sort.Direction.fromString(parts[1].trim());
		} catch (IllegalArgumentException exception) {
			throw new InvalidDocumentListParameterException("sort", "A ordenação informada é inválida.");
		}
		Sort sort = Sort.by(direction, parts[0].trim()).and(Sort.by(Sort.Direction.ASC, "id"));
		return PageRequest.of(query.page(), query.size(), sort);
	}

	private String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		} else {
			return value.trim();
		}
	}

	private DocumentResponse toResponse(Document document) {
		return new DocumentResponse(
				document.getId(),
				document.getTitle(),
				document.getDescription(),
				document.getTags(),
				document.getStatus(),
				document.getTenantId(),
				document.getOwnerId(),
				document.getCreatedAt(),
				document.getUpdatedAt());
	}
}
