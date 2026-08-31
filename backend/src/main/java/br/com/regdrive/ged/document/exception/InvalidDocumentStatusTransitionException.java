package br.com.regdrive.ged.document.exception;

import br.com.regdrive.ged.document.domain.DocumentStatus;

public class InvalidDocumentStatusTransitionException extends RuntimeException {

	public InvalidDocumentStatusTransitionException(DocumentStatus current, DocumentStatus requested) {
		super("Transição de status de %s para %s não permitida.".formatted(current, requested));
	}
}
