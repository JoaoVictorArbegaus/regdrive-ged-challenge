package br.com.regdrive.ged.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record CreateDocumentRequest(
		@NotBlank @Size(max = 255) String title,
		@Size(max = 10000) String description,
		Set<@NotBlank @Size(max = 100) String> tags,
		@Size(max = 100) String tenantId,
		UUID ownerId) {
}
