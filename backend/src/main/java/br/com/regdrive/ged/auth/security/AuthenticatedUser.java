package br.com.regdrive.ged.auth.security;

import br.com.regdrive.ged.user.domain.Role;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public record AuthenticatedUser(
		UUID userId,
		String username,
		Role role,
		String tenantId) {

	public static AuthenticatedUser from(Jwt jwt) {
		return new AuthenticatedUser(
				UUID.fromString(jwt.getClaimAsString("user_id")),
				jwt.getSubject(),
				Role.valueOf(jwt.getClaimAsString("role")),
				jwt.getClaimAsString("tenant_id"));
	}

	public boolean isAdmin() {
		return role == Role.ADMIN;
	}
}
