package br.com.regdrive.ged.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.regdrive.ged.audit.domain.AuditAction;
import br.com.regdrive.ged.audit.service.AuditService;
import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import br.com.regdrive.ged.document.domain.Document;
import br.com.regdrive.ged.document.domain.DocumentVersion;
import br.com.regdrive.ged.document.dto.DocumentVersionResponse;
import br.com.regdrive.ged.document.exception.DocumentNotFoundException;
import br.com.regdrive.ged.document.exception.InvalidFileException;
import br.com.regdrive.ged.document.repository.DocumentRepository;
import br.com.regdrive.ged.document.repository.DocumentVersionRepository;
import br.com.regdrive.ged.document.storage.FileStorage;
import br.com.regdrive.ged.user.domain.Role;
import java.nio.charset.StandardCharsets;
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
	void consecutiveUploadsIncrementVersionAndKeepSha256Checksum() {
		AuthenticatedUser user = user(Role.USER, "tenant-demo");
		Document document = document(user);
		when(documentRepository.findByIdAndTenantId(document.getId(), "tenant-demo"))
				.thenReturn(Optional.of(document));
		when(versionRepository.findMaxVersionNumber(document.getId())).thenReturn(null, 1);
		when(fileStorage.store(any(byte[].class))).thenReturn("first-key", "second-key");
		when(versionRepository.saveAndFlush(any(DocumentVersion.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		DocumentVersionResponse first = versionService.upload(document.getId(), pdfFile(), user);
		DocumentVersionResponse second = versionService.upload(document.getId(), pdfFile(), user);

		assertThat(first.versionNumber()).isEqualTo(1);
		assertThat(second.versionNumber()).isEqualTo(2);
		assertThat(first.checksum())
				.isEqualTo("4fbf22661c0285c55f8fa7518f954910e8b0c33750658fc6863f66aa6b94c7fd");
		assertThat(second.checksum()).isEqualTo(first.checksum());
		verify(auditService, times(2)).record(
				eq(document.getId()), eq(user), eq(AuditAction.FILE_UPLOADED), any());
	}

	@Test
	void uploadRejectsInvalidContentAndViewerPermission() {
		AuthenticatedUser user = user(Role.USER, "tenant-demo");
		Document document = document(user);
		when(documentRepository.findByIdAndTenantId(document.getId(), "tenant-demo"))
				.thenReturn(Optional.of(document));
		MockMultipartFile invalid = new MockMultipartFile(
				"file", "contract.pdf", "application/pdf", "not-pdf".getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> versionService.upload(document.getId(), invalid, user))
				.isInstanceOf(InvalidFileException.class);

		AuthenticatedUser viewer = user(Role.VIEWER, "tenant-demo");
		assertThatThrownBy(() -> versionService.upload(document.getId(), pdfFile(), viewer))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void viewerDownloadsOwnTenantVersionAndCannotSeeAnotherTenant() {
		AuthenticatedUser viewer = user(Role.VIEWER, "tenant-demo");
		Document document = document(viewer);
		DocumentVersion version = new DocumentVersion(
				document.getId(), 1, "file-key", "contract.pdf", "application/pdf",
				PDF_CONTENT.length, "checksum", viewer.userId());
		when(documentRepository.findByIdAndTenantId(document.getId(), "tenant-demo"))
				.thenReturn(Optional.of(document));
		when(versionRepository.findByDocumentIdAndVersionNumber(document.getId(), 1))
				.thenReturn(Optional.of(version));
		when(fileStorage.load("file-key")).thenReturn(PDF_CONTENT);

		var download = versionService.download(document.getId(), 1, viewer);

		assertThat(download.content()).isEqualTo(PDF_CONTENT);
		verify(auditService).record(
				eq(document.getId()), eq(viewer), eq(AuditAction.FILE_DOWNLOADED), any());

		UUID otherTenantDocument = UUID.randomUUID();
		when(documentRepository.findByIdAndTenantId(otherTenantDocument, "tenant-demo"))
				.thenReturn(Optional.empty());
		assertThatThrownBy(() -> versionService.download(otherTenantDocument, 1, viewer))
				.isInstanceOf(DocumentNotFoundException.class);
	}

	private AuthenticatedUser user(Role role, String tenantId) {
		return new AuthenticatedUser(UUID.randomUUID(), role.name().toLowerCase(), role, tenantId);
	}

	private Document document(AuthenticatedUser owner) {
		return new Document("Contract", null, Set.of(), owner.tenantId(), owner.userId());
	}

	private MockMultipartFile pdfFile() {
		return new MockMultipartFile("file", "contract.pdf", "application/pdf", PDF_CONTENT);
	}
}
