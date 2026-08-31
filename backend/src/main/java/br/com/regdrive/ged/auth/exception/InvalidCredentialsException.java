package br.com.regdrive.ged.auth.exception;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Usuário ou senha inválidos.");
	}
}
