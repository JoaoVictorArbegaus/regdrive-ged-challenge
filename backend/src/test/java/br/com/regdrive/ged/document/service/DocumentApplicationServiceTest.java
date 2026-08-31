package br.com.regdrive.ged.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import br.com.regdrive.ged.document.repository.DocumentRepository;
import br.com.regdrive.ged.user.domain.Role;
import br.com.regdrive.ged.user.repository.UserAccountRepository;
import java.time.Instant;
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
	void userCreationUsesAuthenticatedTenantAndOwner() {
		UUID userId = UUID.randomUUID();
		AuthenticatedUser user = new AuthenticatedUser(userId, "user", Role.USER, "tenant-demo");
		CreateDocumentRequest request = new CreateDocumentRequest(
				" Contrato ",
				" Descrição ",
				Set.of("juridico"),
				"forged-tenant",
				UUID.randomUUID());
		when(userRepository.existsById(userId)).thenReturn(true);
		when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DocumentResponse response = documentService.create(request, user);

		assertThat(response.title()).isEqualTo("Contrato");
		assertThat(response.description()).isEqualTo("Descrição");
		assertThat(response.tenantId()).isEqualTo("tenant-demo");
		assertThat(response.ownerId()).isEqualTo(userId);
		assertThat(response.status()).isEqualTo(DocumentStatus.DRAFT);
		verify(auditService).record(
				eq(response.id()), eq(user), eq(AuditAction.DOCUMENT_CREATED), any());
	}

	@Test
	void adminCreationUsesRequestedTenantAndOwner() {
		UUID adminId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		AuthenticatedUser admin = new AuthenticatedUser(adminId, "admin", Role.ADMIN, "tenant-admin");
		CreateDocumentRequest request = new CreateDocumentRequest(
				"Contrato",
				null,
				Set.of(),
				"tenant-client",
				ownerId);
		when(userRepository.existsById(ownerId)).thenReturn(true);
		when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DocumentResponse response = documentService.create(request, admin);

		assertThat(response.tenantId()).isEqualTo("tenant-client");
		assertThat(response.ownerId()).isEqualTo(ownerId);
	}

	@Test
	void viewerCannotCreateDocument() {
		AuthenticatedUser viewer = new AuthenticatedUser(
				UUID.randomUUID(), "viewer", Role.VIEWER, "tenant-demo");
		CreateDocumentRequest request = new CreateDocumentRequest("Contrato", null, Set.of(), null, null);

		assertThatThrownBy(() -> documentService.create(request, viewer))
				.isInstanceOf(AccessDeniedException.class);
		verifyNoInteractions(documentRepository, userRepository);
	}

	@Test
	void userCannotFindDocumentFromAnotherTenant() {
		UUID documentId = UUID.randomUUID();
		AuthenticatedUser user = new AuthenticatedUser(
				UUID.randomUUID(), "user", Role.USER, "tenant-demo");
		when(documentRepository.findByIdAndTenantId(documentId, "tenant-demo"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> documentService.findById(documentId, user))
				.isInstanceOf(DocumentNotFoundException.class);
		verify(documentRepository, never()).findById(documentId);
	}

	@Test
	void userListIgnoresRequestedTenantAndUsesRequestedPagination() {
		AuthenticatedUser user = new AuthenticatedUser(
				UUID.randomUUID(), "user", Role.USER, "tenant-demo");
		DocumentListQuery query = new DocumentListQuery(
				null, null, null, null, null, null, "forged-tenant", 2, 10, "title,asc");
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		when(documentRepository.search(
				eq("tenant-demo"),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				any(Pageable.class)))
				.thenReturn(Page.empty());

		DocumentPageResponse response = documentService.list(query, user);

		verify(documentRepository).search(
				eq("tenant-demo"),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				isNull(),
				pageableCaptor.capture());
		Pageable pageable = pageableCaptor.getValue();
		assertThat(response.content()).isEmpty();
		assertThat(pageable.getPageNumber()).isEqualTo(2);
		assertThat(pageable.getPageSize()).isEqualTo(10);
		assertThat(pageable.getSort().getOrderFor("title").getDirection().isAscending()).isTrue();
		assertThat(pageable.getSort().getOrderFor("id").getDirection().isAscending()).isTrue();
	}

	@Test
	void listRejectsInvalidCreationPeriod() {
		AuthenticatedUser admin = new AuthenticatedUser(
				UUID.randomUUID(), "admin", Role.ADMIN, "tenant-admin");
		DocumentListQuery query = new DocumentListQuery(
				null,
				null,
				null,
				Instant.parse("2026-08-31T12:00:00Z"),
				Instant.parse("2026-08-30T12:00:00Z"),
				null,
				null,
				0,
				20,
				null);

		assertThatThrownBy(() -> documentService.list(query, admin))
				.isInstanceOf(InvalidDocumentListParameterException.class)
				.hasMessage("O período de criação informado é inválido.");
		verifyNoInteractions(documentRepository, userRepository);
	}

	@Test
	void userUpdatesMetadataFromOwnTenant() {
		UUID documentId = UUID.randomUUID();
		AuthenticatedUser user = new AuthenticatedUser(
				UUID.randomUUID(), "user", Role.USER, "tenant-demo");
		Document document = new Document(
				"Contrato", "Descrição", Set.of("antiga"), "tenant-demo", user.userId());
		when(documentRepository.findByIdAndTenantId(documentId, "tenant-demo"))
				.thenReturn(Optional.of(document));

		DocumentResponse response = documentService.updateMetadata(
				documentId,
				new UpdateDocumentRequest("Atualizado", null, Set.of()),
				user);

		assertThat(response.title()).isEqualTo("Atualizado");
		assertThat(response.description()).isNull();
		assertThat(response.tags()).isEmpty();
		verify(auditService).record(
				eq(documentId), eq(user), eq(AuditAction.DOCUMENT_UPDATED), any());
	}

	@Test
	void adminChangesStatusAcrossTenants() {
		UUID documentId = UUID.randomUUID();
		AuthenticatedUser admin = new AuthenticatedUser(
				UUID.randomUUID(), "admin", Role.ADMIN, "tenant-admin");
		Document document = new Document(
				"Contrato", null, Set.of(), "tenant-client", UUID.randomUUID());
		when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

		DocumentResponse response = documentService.updateStatus(
				documentId,
				new UpdateDocumentStatusRequest(DocumentStatus.PUBLISHED),
				admin);

		assertThat(response.status()).isEqualTo(DocumentStatus.PUBLISHED);
		verify(auditService).record(
				eq(documentId), eq(admin), eq(AuditAction.DOCUMENT_PUBLISHED), any());
	}

	@Test
	void viewerCannotChangeStatus() {
		AuthenticatedUser viewer = new AuthenticatedUser(
				UUID.randomUUID(), "viewer", Role.VIEWER, "tenant-demo");

		assertThatThrownBy(() -> documentService.updateStatus(
				UUID.randomUUID(),
				new UpdateDocumentStatusRequest(DocumentStatus.PUBLISHED),
				viewer))
				.isInstanceOf(AccessDeniedException.class);
		verifyNoInteractions(documentRepository, userRepository);
	}
}
