package br.com.regdrive.ged.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.regdrive.ged.audit.domain.AuditAction;
import br.com.regdrive.ged.audit.domain.AuditEvent;
import br.com.regdrive.ged.audit.repository.AuditEventRepository;
import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import br.com.regdrive.ged.document.exception.DocumentNotFoundException;
import br.com.regdrive.ged.document.repository.DocumentRepository;
import br.com.regdrive.ged.user.domain.Role;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditApplicationServiceTest {

	@Mock
	private AuditEventRepository auditRepository;

	@Mock
	private DocumentRepository documentRepository;

	@InjectMocks
	private AuditApplicationService auditService;

	@Test
	void listReturnsVisibleTenantEventsAndHidesOtherTenant() {
		UUID documentId = UUID.randomUUID();
		AuthenticatedUser viewer = new AuthenticatedUser(
				UUID.randomUUID(), "viewer", Role.VIEWER, "tenant-demo");
		AuditEvent created = new AuditEvent(
				documentId, viewer.userId(), AuditAction.DOCUMENT_CREATED, Map.of());
		AuditEvent downloaded = new AuditEvent(
				documentId, viewer.userId(), AuditAction.FILE_DOWNLOADED, Map.of("versionNumber", 1));
		when(documentRepository.existsByIdAndTenantId(documentId, "tenant-demo")).thenReturn(true);
		when(auditRepository.findAllByDocumentIdOrderByOccurredAtAsc(documentId))
				.thenReturn(List.of(created, downloaded));

		var response = auditService.list(documentId, viewer);

		assertThat(response).extracting(event -> event.action())
				.containsExactly(AuditAction.DOCUMENT_CREATED, AuditAction.FILE_DOWNLOADED);

		UUID otherTenantDocument = UUID.randomUUID();
		when(documentRepository.existsByIdAndTenantId(otherTenantDocument, "tenant-demo"))
				.thenReturn(false);
		assertThatThrownBy(() -> auditService.list(otherTenantDocument, viewer))
				.isInstanceOf(DocumentNotFoundException.class);
		verify(auditRepository).findAllByDocumentIdOrderByOccurredAtAsc(documentId);
		verifyNoMoreInteractions(auditRepository);
	}
}
