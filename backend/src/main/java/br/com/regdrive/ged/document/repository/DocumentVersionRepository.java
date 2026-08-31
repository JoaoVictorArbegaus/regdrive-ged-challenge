package br.com.regdrive.ged.document.repository;

import br.com.regdrive.ged.document.domain.DocumentVersion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {

	@Query("select max(version.versionNumber) from DocumentVersion version where version.documentId = :documentId")
	Integer findMaxVersionNumber(@Param("documentId") UUID documentId);
}
