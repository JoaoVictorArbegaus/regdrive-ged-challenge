package br.com.regdrive.ged.document.exception;

public class DocumentArchivedException extends RuntimeException {

	public DocumentArchivedException() {
		super("Documentos arquivados são somente leitura.");
	}
}
