package br.com.regdrive.ged.audit.service;

import br.com.regdrive.ged.audit.domain.AuditAction;
import br.com.regdrive.ged.audit.domain.AuditEvent;
import br.com.regdrive.ged.audit.dto.AuditEventResponse;
import br.com.regdrive.ged.audit.repository.AuditEventRepository;
import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import br.com.regdrive.ged.document.exception.DocumentNotFoundException;
import br.com.regdrive.ged.document.repository.DocumentRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuditApplicationService implements AuditService {

	private final AuditEventRepository auditRepository;
	private final DocumentRepository documentRepository;

	@Override
	@Transactional
	public void record(
			UUID documentId,
			AuthenticatedUser authenticatedUser,
			AuditAction action,
			Map<String, Object> metadata) {
		auditRepository.save(new AuditEvent(
				documentId, authenticatedUser.userId(), action, metadata));
	}

	@Override
	public List<AuditEventResponse> list(
			UUID documentId, AuthenticatedUser authenticatedUser) {
		ensureDocumentIsAccessible(documentId, authenticatedUser);
		return auditRepository.findAllByDocumentIdOrderByOccurredAtAsc(documentId).stream()
				.map(this::toResponse)
				.toList();
	}

	private void ensureDocumentIsAccessible(
			UUID documentId, AuthenticatedUser authenticatedUser) {
		boolean exists;
		if (authenticatedUser.isAdmin()) {
			exists = documentRepository.existsById(documentId);
		} else {
			exists = documentRepository.existsByIdAndTenantId(
					documentId, authenticatedUser.tenantId());
		}
		if (!exists) {
			throw new DocumentNotFoundException();
		}
	}

	private AuditEventResponse toResponse(AuditEvent event) {
		return new AuditEventResponse(
				event.getId(),
				event.getDocumentId(),
				event.getUserId(),
				event.getAction(),
				event.getMetadata(),
				event.getOccurredAt());
	}
}
