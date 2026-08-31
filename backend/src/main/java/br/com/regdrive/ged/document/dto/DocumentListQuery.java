package br.com.regdrive.ged.document.dto;

import br.com.regdrive.ged.document.domain.DocumentStatus;
import java.time.Instant;
import java.util.UUID;

public record DocumentListQuery(
		String title,
		String tag,
		DocumentStatus status,
		Instant createdFrom,
		Instant createdTo,
		UUID ownerId,
		String tenantId,
		int page,
		int size,
		String sort) {
}
