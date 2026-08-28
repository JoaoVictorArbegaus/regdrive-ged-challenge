package br.com.regdrive.ged.auth;

import br.com.regdrive.ged.user.UserAccount;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final Duration tokenTtl;

	public JwtTokenService(
			JwtEncoder jwtEncoder,
			@Value("${app.security.jwt.ttl}") Duration tokenTtl) {
		this.jwtEncoder = jwtEncoder;
		this.tokenTtl = tokenTtl;
	}

	LoginResponse issueToken(UserAccount user) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(tokenTtl);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("regdrive-ged")
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(user.getUsername())
				.claim("user_id", user.getId().toString())
				.claim("role", user.getRole().name())
				.claim("tenant_id", user.getTenantId())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

		return new LoginResponse(token, "Bearer", expiresAt);
	}
}
