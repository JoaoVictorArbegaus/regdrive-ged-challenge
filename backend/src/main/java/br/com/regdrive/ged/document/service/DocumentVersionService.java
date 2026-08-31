package br.com.regdrive.ged.document.service;

import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import br.com.regdrive.ged.document.dto.DocumentVersionDownload;
import br.com.regdrive.ged.document.dto.DocumentVersionResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentVersionService {

	DocumentVersionResponse upload(
			UUID documentId, MultipartFile file, AuthenticatedUser authenticatedUser);

	List<DocumentVersionResponse> list(UUID documentId, AuthenticatedUser authenticatedUser);

	DocumentVersionResponse findByVersionNumber(
			UUID documentId, int versionNumber, AuthenticatedUser authenticatedUser);

	DocumentVersionDownload download(
			UUID documentId, int versionNumber, AuthenticatedUser authenticatedUser);
}
