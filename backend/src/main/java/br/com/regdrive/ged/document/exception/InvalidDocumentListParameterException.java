package br.com.regdrive.ged.document.exception;

public class InvalidDocumentListParameterException extends RuntimeException {

	private final String parameter;

	public InvalidDocumentListParameterException(String parameter, String message) {
		super(message);
		this.parameter = parameter;
	}

	public String getParameter() {
		return parameter;
	}
}
