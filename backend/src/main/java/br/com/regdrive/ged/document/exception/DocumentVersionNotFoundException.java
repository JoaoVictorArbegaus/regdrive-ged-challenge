package br.com.regdrive.ged.document.exception;

public class DocumentVersionNotFoundException extends RuntimeException {

	public DocumentVersionNotFoundException() {
		super("Versão do documento não encontrada.");
	}
}
