package br.com.regdrive.ged.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank String username,
		@NotBlank String password) {
}
