package br.com.regdrive.ged.document.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {

	@Id
	private UUID id;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DocumentStatus status;

	@Column(name = "tenant_id", nullable = false, length = 100)
	private String tenantId;

	@Column(name = "owner_id", nullable = false)
	private UUID ownerId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "document_tags", joinColumns = @JoinColumn(name = "document_id"))
	@Column(name = "tag", nullable = false, length = 100)
	private Set<String> tags = new LinkedHashSet<>();

	public Document(
			String title,
			String description,
			Set<String> tags,
			String tenantId,
			UUID ownerId) {
		Instant now = Instant.now();
		this.id = UUID.randomUUID();
		this.title = title.trim();
		this.description = normalizeDescription(description);
		this.tags = normalizeTags(tags);
		this.status = DocumentStatus.DRAFT;
		this.tenantId = tenantId;
		this.ownerId = ownerId;
		this.createdAt = now;
		this.updatedAt = now;
	}

	private String normalizeDescription(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private Set<String> normalizeTags(Set<String> values) {
		if (values == null) {
			return new LinkedHashSet<>();
		}
		Set<String> normalized = new LinkedHashSet<>();
		values.forEach(value -> normalized.add(value.trim()));
		return normalized;
	}

	public Set<String> getTags() {
		return Set.copyOf(tags);
	}
}
