package br.com.regdrive.ged.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
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
	void viewerListsAuditFromOwnTenant() {
		UUID documentId = UUID.randomUUID();
		AuthenticatedUser viewer = user(Role.VIEWER, "tenant-demo");
		AuditEvent event = new AuditEvent(
				documentId,
				viewer.userId(),
				AuditAction.FILE_DOWNLOADED,
				Map.of("versionNumber", 1));
		when(documentRepository.existsByIdAndTenantId(documentId, "tenant-demo")).thenReturn(true);
		when(auditRepository.findAllByDocumentIdOrderByOccurredAtAsc(documentId))
				.thenReturn(List.of(event));

		var response = auditService.list(documentId, viewer);

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().action()).isEqualTo(AuditAction.FILE_DOWNLOADED);
		assertThat(response.getFirst().metadata()).containsEntry("versionNumber", 1);
	}

	@Test
	void userCannotListAuditFromAnotherTenant() {
		UUID documentId = UUID.randomUUID();
		AuthenticatedUser user = user(Role.USER, "tenant-demo");
		when(documentRepository.existsByIdAndTenantId(documentId, "tenant-demo")).thenReturn(false);

		assertThatThrownBy(() -> auditService.list(documentId, user))
				.isInstanceOf(DocumentNotFoundException.class);
		verifyNoInteractions(auditRepository);
	}

	private AuthenticatedUser user(Role role, String tenantId) {
		return new AuthenticatedUser(UUID.randomUUID(), role.name().toLowerCase(), role, tenantId);
	}
}
