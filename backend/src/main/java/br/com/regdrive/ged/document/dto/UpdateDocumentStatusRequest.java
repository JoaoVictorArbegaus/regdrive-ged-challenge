package br.com.regdrive.ged.document.dto;

import br.com.regdrive.ged.document.domain.DocumentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDocumentStatusRequest(
		@NotNull DocumentStatus status) {
}
