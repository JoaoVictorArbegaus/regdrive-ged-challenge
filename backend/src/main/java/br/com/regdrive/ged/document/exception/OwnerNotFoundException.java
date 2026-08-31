package br.com.regdrive.ged.document.exception;

public class OwnerNotFoundException extends RuntimeException {

	public OwnerNotFoundException() {
		super("Responsável pelo documento não encontrado.");
	}
}
