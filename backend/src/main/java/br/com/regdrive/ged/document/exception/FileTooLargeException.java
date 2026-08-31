package br.com.regdrive.ged.document.exception;

public class FileTooLargeException extends RuntimeException {

	public FileTooLargeException() {
		super("O arquivo não pode exceder 10 MiB.");
	}
}
