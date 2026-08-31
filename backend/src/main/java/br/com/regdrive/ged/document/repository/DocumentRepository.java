package br.com.regdrive.ged.document.repository;

import br.com.regdrive.ged.document.domain.Document;
import br.com.regdrive.ged.document.domain.DocumentStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

	Optional<Document> findByIdAndTenantId(UUID id, String tenantId);

	@Query("""
			select document from Document document
			where (:tenantId is null or document.tenantId = :tenantId)
			  and (:ownerId is null or document.ownerId = :ownerId)
			  and (:status is null or document.status = :status)
			  and (:titlePattern is null or lower(document.title) like :titlePattern)
			  and (:tag is null or :tag member of document.tags)
			  and document.createdAt >= coalesce(:createdFrom, document.createdAt)
			  and document.createdAt <= coalesce(:createdTo, document.createdAt)
			""")
	Page<Document> search(
			@Param("tenantId") String tenantId,
			@Param("ownerId") UUID ownerId,
			@Param("status") DocumentStatus status,
			@Param("titlePattern") String titlePattern,
			@Param("tag") String tag,
			@Param("createdFrom") Instant createdFrom,
			@Param("createdTo") Instant createdTo,
			Pageable pageable);
}
