package br.com.regdrive.ged.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
		name = "document_versions",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_document_versions_number",
				columnNames = {"document_id", "version_number"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentVersion {

	@Id
	private UUID id;

	@Column(name = "document_id", nullable = false)
	private UUID documentId;

	@Column(name = "version_number", nullable = false)
	private int versionNumber;

	@Column(name = "file_key", nullable = false, unique = true, length = 100)
	private String fileKey;

	@Column(name = "original_filename", nullable = false, length = 255)
	private String originalFilename;

	@Column(name = "mime_type", nullable = false, length = 100)
	private String mimeType;

	@Column(name = "file_size", nullable = false)
	private long fileSize;

	@Column(nullable = false, length = 64)
	private String checksum;

	@Column(name = "uploaded_at", nullable = false)
	private Instant uploadedAt;

	@Column(name = "uploaded_by", nullable = false)
	private UUID uploadedBy;

	public DocumentVersion(
			UUID documentId,
			int versionNumber,
			String fileKey,
			String originalFilename,
			String mimeType,
			long fileSize,
			String checksum,
			UUID uploadedBy) {
		this.id = UUID.randomUUID();
		this.documentId = documentId;
		this.versionNumber = versionNumber;
		this.fileKey = fileKey;
		this.originalFilename = originalFilename;
		this.mimeType = mimeType;
		this.fileSize = fileSize;
		this.checksum = checksum;
		this.uploadedAt = Instant.now();
		this.uploadedBy = uploadedBy;
	}
}
