package br.com.regdrive.ged.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditEvent {

	@Id
	private UUID id;

	@Column(name = "document_id", nullable = false)
	private UUID documentId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AuditAction action;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> metadata;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	public AuditEvent(
			UUID documentId,
			UUID userId,
			AuditAction action,
			Map<String, Object> metadata) {
		this.id = UUID.randomUUID();
		this.documentId = documentId;
		this.userId = userId;
		this.action = action;
		this.metadata = Map.copyOf(metadata);
		this.occurredAt = Instant.now();
	}
}
