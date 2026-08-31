package br.com.regdrive.ged.document.service;

import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import br.com.regdrive.ged.document.domain.Document;
import br.com.regdrive.ged.document.dto.CreateDocumentRequest;
import br.com.regdrive.ged.document.dto.DocumentResponse;
import br.com.regdrive.ged.document.exception.DocumentNotFoundException;
import br.com.regdrive.ged.document.exception.OwnerNotFoundException;
import br.com.regdrive.ged.document.repository.DocumentRepository;
import br.com.regdrive.ged.user.domain.Role;
import br.com.regdrive.ged.user.repository.UserAccountRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DocumentApplicationService implements DocumentService {

	private final DocumentRepository documentRepository;
	private final UserAccountRepository userRepository;

	@Override
	@Transactional
	public DocumentResponse create(CreateDocumentRequest request, AuthenticatedUser authenticatedUser) {
		if (authenticatedUser.role() == Role.VIEWER) {
			throw new AccessDeniedException("Usuário sem permissão para criar documentos.");
		}

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
		return toResponse(documentRepository.save(document));
	}

	@Override
	public DocumentResponse findById(UUID documentId, AuthenticatedUser authenticatedUser) {
		Document document = authenticatedUser.isAdmin()
				? documentRepository.findById(documentId).orElseThrow(DocumentNotFoundException::new)
				: documentRepository.findByIdAndTenantId(documentId, authenticatedUser.tenantId())
						.orElseThrow(DocumentNotFoundException::new);
		return toResponse(document);
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
