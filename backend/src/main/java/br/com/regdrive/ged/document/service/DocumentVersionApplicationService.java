package br.com.regdrive.ged.document.service;

import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import br.com.regdrive.ged.document.domain.Document;
import br.com.regdrive.ged.document.domain.DocumentStatus;
import br.com.regdrive.ged.document.domain.DocumentVersion;
import br.com.regdrive.ged.document.dto.DocumentVersionResponse;
import br.com.regdrive.ged.document.exception.DocumentArchivedException;
import br.com.regdrive.ged.document.exception.DocumentNotFoundException;
import br.com.regdrive.ged.document.exception.FileStorageException;
import br.com.regdrive.ged.document.exception.FileTooLargeException;
import br.com.regdrive.ged.document.exception.InvalidFileException;
import br.com.regdrive.ged.document.repository.DocumentRepository;
import br.com.regdrive.ged.document.repository.DocumentVersionRepository;
import br.com.regdrive.ged.document.storage.FileStorage;
import br.com.regdrive.ged.user.domain.Role;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DocumentVersionApplicationService implements DocumentVersionService {

	private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
	private static final byte[] PNG_SIGNATURE = {
			(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
	};

	private final DocumentRepository documentRepository;
	private final DocumentVersionRepository versionRepository;
	private final FileStorage fileStorage;

	@Override
	@Transactional
	public DocumentVersionResponse upload(
			UUID documentId, MultipartFile file, AuthenticatedUser authenticatedUser) {
		ensureCanUpload(authenticatedUser);
		Document document = findAccessibleDocument(documentId, authenticatedUser);
		if (document.getStatus() == DocumentStatus.ARCHIVED) {
			throw new DocumentArchivedException();
		}

		ValidatedFile validatedFile = validate(file);
		String checksum = calculateChecksum(validatedFile.content());
		Integer currentVersion = versionRepository.findMaxVersionNumber(documentId);
		int nextVersion;
		if (currentVersion == null) {
			nextVersion = 1;
		} else {
			nextVersion = currentVersion + 1;
		}

		String fileKey = fileStorage.store(validatedFile.content());
		DocumentVersion version = new DocumentVersion(
				documentId,
				nextVersion,
				fileKey,
				validatedFile.filename(),
				validatedFile.mimeType(),
				validatedFile.content().length,
				checksum,
				authenticatedUser.userId());
		try {
			return toResponse(versionRepository.saveAndFlush(version));
		} catch (RuntimeException exception) {
			try {
				fileStorage.delete(fileKey);
			} catch (FileStorageException cleanupException) {
				exception.addSuppressed(cleanupException);
			}
			throw exception;
		}
	}

	private void ensureCanUpload(AuthenticatedUser authenticatedUser) {
		if (authenticatedUser.role() == Role.VIEWER) {
			throw new AccessDeniedException("Usuário sem permissão para enviar arquivos.");
		}
	}

	private Document findAccessibleDocument(
			UUID documentId, AuthenticatedUser authenticatedUser) {
		if (authenticatedUser.isAdmin()) {
			return documentRepository.findById(documentId)
					.orElseThrow(DocumentNotFoundException::new);
		} else {
			return documentRepository.findByIdAndTenantId(documentId, authenticatedUser.tenantId())
					.orElseThrow(DocumentNotFoundException::new);
		}
	}

	private ValidatedFile validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidFileException("O arquivo é obrigatório.");
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new FileTooLargeException();
		}

		String filename = extractFilename(file.getOriginalFilename());
		String mimeType = file.getContentType();
		byte[] content;
		try {
			content = file.getBytes();
		} catch (IOException exception) {
			throw new FileStorageException("Não foi possível ler o arquivo recebido.", exception);
		}
		if (content.length == 0) {
			throw new InvalidFileException("O arquivo é obrigatório.");
		}
		if (content.length > MAX_FILE_SIZE) {
			throw new FileTooLargeException();
		}

		String extension = extensionOf(filename);
		boolean valid;
		if (extension.equals("pdf")) {
			valid = "application/pdf".equals(mimeType) && hasPdfSignature(content);
		} else if (extension.equals("png")) {
			valid = "image/png".equals(mimeType) && hasPngSignature(content);
		} else if (extension.equals("jpg") || extension.equals("jpeg")) {
			valid = "image/jpeg".equals(mimeType) && hasJpegSignature(content);
		} else {
			valid = false;
		}
		if (!valid) {
			throw new InvalidFileException("Somente arquivos PDF, PNG e JPG válidos são permitidos.");
		}
		return new ValidatedFile(filename, mimeType, content);
	}

	private String extractFilename(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) {
			throw new InvalidFileException("O nome do arquivo é obrigatório.");
		}
		String normalized = originalFilename.replace('\\', '/');
		int separator = normalized.lastIndexOf('/');
		String filename;
		if (separator >= 0) {
			filename = normalized.substring(separator + 1);
		} else {
			filename = normalized;
		}
		if (filename.isBlank() || filename.length() > 255) {
			throw new InvalidFileException("O nome do arquivo é inválido.");
		}
		return filename;
	}

	private String extensionOf(String filename) {
		int separator = filename.lastIndexOf('.');
		if (separator <= 0 || separator == filename.length() - 1) {
			return "";
		}
		return filename.substring(separator + 1).toLowerCase(Locale.ROOT);
	}

	private boolean hasPdfSignature(byte[] content) {
		return content.length >= 5
				&& content[0] == '%'
				&& content[1] == 'P'
				&& content[2] == 'D'
				&& content[3] == 'F'
				&& content[4] == '-';
	}

	private boolean hasPngSignature(byte[] content) {
		if (content.length < PNG_SIGNATURE.length) {
			return false;
		}
		for (int index = 0; index < PNG_SIGNATURE.length; index++) {
			if (content[index] != PNG_SIGNATURE[index]) {
				return false;
			}
		}
		return true;
	}

	private boolean hasJpegSignature(byte[] content) {
		return content.length >= 3
				&& content[0] == (byte) 0xFF
				&& content[1] == (byte) 0xD8
				&& content[2] == (byte) 0xFF;
	}

	private String calculateChecksum(byte[] content) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(content));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 não está disponível.", exception);
		}
	}

	private DocumentVersionResponse toResponse(DocumentVersion version) {
		return new DocumentVersionResponse(
				version.getId(),
				version.getDocumentId(),
				version.getVersionNumber(),
				version.getOriginalFilename(),
				version.getMimeType(),
				version.getFileSize(),
				version.getChecksum(),
				version.getUploadedAt(),
				version.getUploadedBy());
	}

	private record ValidatedFile(String filename, String mimeType, byte[] content) {
	}
}
