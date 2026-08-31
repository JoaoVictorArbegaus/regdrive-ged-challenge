package br.com.regdrive.ged.document.exception;

public class DocumentNotFoundException extends RuntimeException {

	public DocumentNotFoundException() {
		super("Documento não encontrado.");
	}
}
