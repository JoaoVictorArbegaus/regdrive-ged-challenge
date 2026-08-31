package br.com.regdrive.ged.document.repository;

import br.com.regdrive.ged.document.domain.Document;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

	Optional<Document> findByIdAndTenantId(UUID id, String tenantId);
}
