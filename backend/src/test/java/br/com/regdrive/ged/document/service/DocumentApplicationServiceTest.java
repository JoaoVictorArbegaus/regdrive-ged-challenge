package br.com.regdrive.ged.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import br.com.regdrive.ged.document.domain.Document;
import br.com.regdrive.ged.document.domain.DocumentStatus;
import br.com.regdrive.ged.document.dto.CreateDocumentRequest;
import br.com.regdrive.ged.document.dto.DocumentResponse;
import br.com.regdrive.ged.document.exception.DocumentNotFoundException;
import br.com.regdrive.ged.document.repository.DocumentRepository;
import br.com.regdrive.ged.user.domain.Role;
import br.com.regdrive.ged.user.repository.UserAccountRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class DocumentApplicationServiceTest {

	@Mock
	private DocumentRepository documentRepository;

	@Mock
	private UserAccountRepository userRepository;

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
}
