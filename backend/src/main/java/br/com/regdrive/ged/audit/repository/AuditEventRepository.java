package br.com.regdrive.ged.audit.repository;

import br.com.regdrive.ged.audit.domain.AuditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

	List<AuditEvent> findAllByDocumentIdOrderByOccurredAtAsc(UUID documentId);
}
