package br.com.regdrive.ged.audit.service;

import br.com.regdrive.ged.audit.domain.AuditAction;
import br.com.regdrive.ged.audit.dto.AuditEventResponse;
import br.com.regdrive.ged.auth.security.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AuditService {

	void record(
			UUID documentId,
			AuthenticatedUser authenticatedUser,
			AuditAction action,
			Map<String, Object> metadata);

	List<AuditEventResponse> list(UUID documentId, AuthenticatedUser authenticatedUser);
}
