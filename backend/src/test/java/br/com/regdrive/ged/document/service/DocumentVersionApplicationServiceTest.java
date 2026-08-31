package br.com.regdrive.ged.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.regdrive.ged.audit.domain.AuditAction;
import br.com.regdrive.ged.audit.service.AuditService;
import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import br.com.regdrive.ged.document.domain.Document;
import br.com.regdrive.ged.document.domain.DocumentStatus;
import br.com.regdrive.ged.document.domain.DocumentVersion;
import br.com.regdrive.ged.document.dto.DocumentVersionResponse;
import br.com.regdrive.ged.document.exception.DocumentArchivedException;
import br.com.regdrive.ged.document.exception.DocumentNotFoundException;
import br.com.regdrive.ged.document.exception.InvalidFileException;
import br.com.regdrive.ged.document.repository.DocumentRepository;
import br.com.regdrive.ged.document.repository.DocumentVersionRepository;
import br.com.regdrive.ged.document.storage.FileStorage;
import br.com.regdrive.ged.user.domain.Role;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class DocumentVersionApplicationServiceTest {

	private static final byte[] PDF_CONTENT = "%PDF-1.4\ncontent".getBytes(StandardCharsets.UTF_8);

	@Mock
	private DocumentRepository documentRepository;

	@Mock
	private DocumentVersionRepository versionRepository;

	@Mock
	private FileStorage fileStorage;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private DocumentVersionApplicationService versionService;

	@Test
	void uploadCreatesFirstVersionWithSha256Checksum() {
		AuthenticatedUser user = user(Role.USER, "tenant-demo");
		Document document = document(user);
		MockMultipartFile file = pdfFile();
		when(documentRepository.findByIdAndTenantId(document.getId(), "tenant-demo"))
				.thenReturn(Optional.of(document));
		when(versionRepository.findMaxVersionNumber(document.getId())).thenReturn(null);
		when(fileStorage.store(PDF_CONTENT)).thenReturn("file-key");
		when(versionRepository.saveAndFlush(any(DocumentVersion.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		DocumentVersionResponse response = versionService.upload(document.getId(), file, user);

		assertThat(response.versionNumber()).isEqualTo(1);
		assertThat(response.originalFilename()).isEqualTo("contrato.pdf");
		assertThat(response.mimeType()).isEqualTo("application/pdf");
		assertThat(response.fileSize()).isEqualTo(PDF_CONTENT.length);
		assertThat(response.checksum())
				.isEqualTo("4fbf22661c0285c55f8fa7518f954910e8b0c33750658fc6863f66aa6b94c7fd");
		assertThat(response.uploadedBy()).isEqualTo(user.userId());
		verify(auditService).record(
				org.mockito.ArgumentMatchers.eq(document.getId()),
				org.mockito.ArgumentMatchers.eq(user),
				org.mockito.ArgumentMatchers.eq(AuditAction.FILE_UPLOADED),
				any());
	}

	@Test
	void uploadIncrementsLastVersionNumber() {
		AuthenticatedUser user = user(Role.USER, "tenant-demo");
		Document document = document(user);
		when(documentRepository.findByIdAndTenantId(document.getId(), "tenant-demo"))
				.thenReturn(Optional.of(document));
		when(versionRepository.findMaxVersionNumber(document.getId())).thenReturn(1);
		when(fileStorage.store(PDF_CONTENT)).thenReturn("file-key");
		when(versionRepository.saveAndFlush(any(DocumentVersion.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		DocumentVersionResponse response = versionService.upload(document.getId(), pdfFile(), user);

		assertThat(response.versionNumber()).isEqualTo(2);
	}

	@Test
	void uploadRejectsFileWhoseContentDoesNotMatchExtension() {
		AuthenticatedUser user = user(Role.USER, "tenant-demo");
		Document document = document(user);
		MockMultipartFile file = new MockMultipartFile(
				"file", "contrato.pdf", "application/pdf", "not-a-pdf".getBytes(StandardCharsets.UTF_8));
		when(documentRepository.findByIdAndTenantId(document.getId(), "tenant-demo"))
				.thenReturn(Optional.of(document));

		assertThatThrownBy(() -> versionService.upload(document.getId(), file, user))
				.isInstanceOf(InvalidFileException.class);
		verifyNoInteractions(versionRepository, fileStorage);
	}

	@Test
	void viewerCannotUploadFile() {
		AuthenticatedUser viewer = user(Role.VIEWER, "tenant-demo");

		assertThatThrownBy(() -> versionService.upload(UUID.randomUUID(), pdfFile(), viewer))
				.isInstanceOf(AccessDeniedException.class);
		verifyNoInteractions(documentRepository, versionRepository, fileStorage);
	}

	@Test
	void userCannotUploadToDocumentFromAnotherTenant() {
		AuthenticatedUser user = user(Role.USER, "tenant-demo");
		UUID documentId = UUID.randomUUID();
		when(documentRepository.findByIdAndTenantId(documentId, "tenant-demo"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> versionService.upload(documentId, pdfFile(), user))
				.isInstanceOf(DocumentNotFoundException.class);
		verifyNoInteractions(versionRepository, fileStorage);
	}

	@Test
	void archivedDocumentRejectsUpload() {
		AuthenticatedUser user = user(Role.USER, "tenant-demo");
		Document document = document(user);
		document.transitionTo(DocumentStatus.PUBLISHED);
		document.transitionTo(DocumentStatus.ARCHIVED);
		when(documentRepository.findByIdAndTenantId(document.getId(), "tenant-demo"))
				.thenReturn(Optional.of(document));

		assertThatThrownBy(() -> versionService.upload(document.getId(), pdfFile(), user))
				.isInstanceOf(DocumentArchivedException.class);
		verifyNoInteractions(versionRepository, fileStorage);
	}

	@Test
	void persistenceFailureRemovesStoredFile() {
		AuthenticatedUser user = user(Role.USER, "tenant-demo");
		Document document = document(user);
		when(documentRepository.findByIdAndTenantId(document.getId(), "tenant-demo"))
				.thenReturn(Optional.of(document));
		when(versionRepository.findMaxVersionNumber(document.getId())).thenReturn(null);
		when(fileStorage.store(PDF_CONTENT)).thenReturn("file-key");
		when(versionRepository.saveAndFlush(any(DocumentVersion.class)))
				.thenThrow(new IllegalStateException("database failure"));

		assertThatThrownBy(() -> versionService.upload(document.getId(), pdfFile(), user))
				.isInstanceOf(IllegalStateException.class);
		verify(fileStorage).delete("file-key");
	}

	@Test
	void viewerListsVersionsFromOwnTenantInRepositoryOrder() {
		AuthenticatedUser viewer = user(Role.VIEWER, "tenant-demo");
		Document document = document(viewer);
		DocumentVersion first = version(document.getId(), 1, viewer.userId(), "first-key");
		DocumentVersion second = version(document.getId(), 2, viewer.userId(), "second-key");
		when(documentRepository.findByIdAndTenantId(document.getId(), "tenant-demo"))
				.thenReturn(Optional.of(document));
		when(versionRepository.findAllByDocumentIdOrderByVersionNumberAsc(document.getId()))
				.thenReturn(List.of(first, second));

		List<DocumentVersionResponse> response = versionService.list(document.getId(), viewer);

		assertThat(response).extracting(DocumentVersionResponse::versionNumber)
				.containsExactly(1, 2);
	}

	@Test
	void downloadLoadsStoredContentAndRecordsAudit() {
		AuthenticatedUser viewer = user(Role.VIEWER, "tenant-demo");
		Document document = document(viewer);
		DocumentVersion version = version(document.getId(), 1, viewer.userId(), "file-key");
		when(documentRepository.findByIdAndTenantId(document.getId(), "tenant-demo"))
				.thenReturn(Optional.of(document));
		when(versionRepository.findByDocumentIdAndVersionNumber(document.getId(), 1))
				.thenReturn(Optional.of(version));
		when(fileStorage.load("file-key")).thenReturn(PDF_CONTENT);

		var response = versionService.download(document.getId(), 1, viewer);

		assertThat(response.content()).isEqualTo(PDF_CONTENT);
		assertThat(response.filename()).isEqualTo("contrato.pdf");
		verify(auditService).record(
				org.mockito.ArgumentMatchers.eq(document.getId()),
				org.mockito.ArgumentMatchers.eq(viewer),
				org.mockito.ArgumentMatchers.eq(AuditAction.FILE_DOWNLOADED),
				any());
	}

	private AuthenticatedUser user(Role role, String tenantId) {
		return new AuthenticatedUser(UUID.randomUUID(), role.name().toLowerCase(), role, tenantId);
	}

	private Document document(AuthenticatedUser owner) {
		return new Document("Contrato", null, Set.of(), owner.tenantId(), owner.userId());
	}

	private MockMultipartFile pdfFile() {
		return new MockMultipartFile("file", "contrato.pdf", "application/pdf", PDF_CONTENT);
	}

	private DocumentVersion version(
			UUID documentId, int versionNumber, UUID uploadedBy, String fileKey) {
		return new DocumentVersion(
				documentId,
				versionNumber,
				fileKey,
				"contrato.pdf",
				"application/pdf",
				PDF_CONTENT.length,
				"checksum",
				uploadedBy);
	}
}
