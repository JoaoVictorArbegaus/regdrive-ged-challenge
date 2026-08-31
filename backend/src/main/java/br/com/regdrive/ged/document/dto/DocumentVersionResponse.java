package br.com.regdrive.ged.document.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentVersionResponse(
		UUID id,
		UUID documentId,
		int versionNumber,
		String originalFilename,
		String mimeType,
		long fileSize,
		String checksum,
		Instant uploadedAt,
		UUID uploadedBy) {
}
