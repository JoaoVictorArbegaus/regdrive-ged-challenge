package br.com.regdrive.ged.audit.dto;

import br.com.regdrive.ged.audit.domain.AuditAction;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
		UUID id,
		UUID documentId,
		UUID userId,
		AuditAction action,
		Map<String, Object> metadata,
		Instant timestamp) {
}
