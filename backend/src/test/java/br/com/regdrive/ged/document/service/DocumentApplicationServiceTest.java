package br.com.regdrive.ged.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.regdrive.ged.audit.domain.AuditAction;
import br.com.regdrive.ged.audit.service.AuditService;
import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import br.com.regdrive.ged.document.domain.Document;
import br.com.regdrive.ged.document.domain.DocumentStatus;
import br.com.regdrive.ged.document.dto.CreateDocumentRequest;
import br.com.regdrive.ged.document.dto.DocumentListQuery;
import br.com.regdrive.ged.document.dto.DocumentResponse;
import br.com.regdrive.ged.document.dto.UpdateDocumentStatusRequest;
import br.com.regdrive.ged.document.exception.InvalidDocumentStatusTransitionException;
import br.com.regdrive.ged.document.repository.DocumentRepository;
import br.com.regdrive.ged.user.domain.Role;
import br.com.regdrive.ged.user.repository.UserAccountRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class DocumentApplicationServiceTest {

	@Mock
	private DocumentRepository documentRepository;

	@Mock
	private UserAccountRepository userRepository;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private DocumentApplicationService documentService;

	@Test
	void createUsesAuthenticatedTenantAndRejectsViewerWrite() {
		AuthenticatedUser user = user(Role.USER, "tenant-demo");
		CreateDocumentRequest request = new CreateDocumentRequest(
				" Contract ", null, Set.of("legal"), "forged-tenant", UUID.randomUUID());
		when(userRepository.existsById(user.userId())).thenReturn(true);
		when(documentRepository.save(any(Document.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		DocumentResponse response = documentService.create(request, user);

		assertThat(response.tenantId()).isEqualTo("tenant-demo");
		assertThat(response.ownerId()).isEqualTo(user.userId());
		verify(auditService).record(
				eq(response.id()), eq(user), eq(AuditAction.DOCUMENT_CREATED), any());

		AuthenticatedUser viewer = user(Role.VIEWER, "tenant-demo");
		assertThatThrownBy(() -> documentService.create(request, viewer))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void listAppliesFiltersAndAlwaysUsesAuthenticatedTenant() {
		AuthenticatedUser user = user(Role.USER, "tenant-demo");
		DocumentListQuery query = new DocumentListQuery(
				" Contract ", "legal", DocumentStatus.DRAFT, null, null, null,
				"forged-tenant", 1, 10, "title,asc");
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		when(documentRepository.search(
				eq("tenant-demo"), isNull(), eq(DocumentStatus.DRAFT), eq("%contract%"),
				eq("legal"), isNull(), isNull(), any(Pageable.class)))
				.thenReturn(Page.empty());

		var response = documentService.list(query, user);

		assertThat(response.content()).isEmpty();
		verify(documentRepository).search(
				eq("tenant-demo"), isNull(), eq(DocumentStatus.DRAFT), eq("%contract%"),
				eq("legal"), isNull(), isNull(), pageable.capture());
		assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
		assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
		assertThat(pageable.getValue().getSort().getOrderFor("title").isAscending()).isTrue();
	}

	@Test
	void statusLifecycleRecordsAuditAndRejectsInvalidTransition() {
		AuthenticatedUser user = user(Role.USER, "tenant-demo");
		Document document = document(user);
		when(documentRepository.findByIdAndTenantId(document.getId(), "tenant-demo"))
				.thenReturn(Optional.of(document));

		documentService.updateStatus(
				document.getId(), new UpdateDocumentStatusRequest(DocumentStatus.PUBLISHED), user);
		documentService.updateStatus(
				document.getId(), new UpdateDocumentStatusRequest(DocumentStatus.ARCHIVED), user);

		assertThat(document.getStatus()).isEqualTo(DocumentStatus.ARCHIVED);
		verify(auditService).record(
				eq(document.getId()), eq(user), eq(AuditAction.DOCUMENT_PUBLISHED), any());
		verify(auditService).record(
				eq(document.getId()), eq(user), eq(AuditAction.DOCUMENT_ARCHIVED), any());

		Document invalidDocument = document(user);
		when(documentRepository.findByIdAndTenantId(invalidDocument.getId(), "tenant-demo"))
				.thenReturn(Optional.of(invalidDocument));
		assertThatThrownBy(() -> documentService.updateStatus(
				invalidDocument.getId(),
				new UpdateDocumentStatusRequest(DocumentStatus.ARCHIVED),
				user))
				.isInstanceOf(InvalidDocumentStatusTransitionException.class);
	}

	private AuthenticatedUser user(Role role, String tenantId) {
		return new AuthenticatedUser(UUID.randomUUID(), role.name().toLowerCase(), role, tenantId);
	}

	private Document document(AuthenticatedUser owner) {
		return new Document("Contract", null, Set.of(), owner.tenantId(), owner.userId());
	}
}
