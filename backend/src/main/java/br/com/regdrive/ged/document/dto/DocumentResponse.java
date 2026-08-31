package br.com.regdrive.ged.document.dto;

import br.com.regdrive.ged.document.domain.DocumentStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record DocumentResponse(
		UUID id,
		String title,
		String description,
		Set<String> tags,
		DocumentStatus status,
		String tenantId,
		UUID ownerId,
		Instant createdAt,
		Instant updatedAt) {
}
