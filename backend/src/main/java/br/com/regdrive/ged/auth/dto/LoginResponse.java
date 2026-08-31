package br.com.regdrive.ged.auth.dto;

import java.time.Instant;

public record LoginResponse(
		String accessToken,
		String tokenType,
		Instant expiresAt) {
}
