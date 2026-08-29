package br.com.regdrive.ged.auth.service;

import br.com.regdrive.ged.auth.dto.LoginRequest;
import br.com.regdrive.ged.auth.dto.LoginResponse;
import br.com.regdrive.ged.auth.exception.InvalidCredentialsException;
import br.com.regdrive.ged.auth.security.JwtTokenService;
import br.com.regdrive.ged.user.domain.UserAccount;
import br.com.regdrive.ged.user.repository.UserAccountRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthApplicationService implements AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserAccountRepository userRepository;
	private final JwtTokenService jwtTokenService;

	public AuthApplicationService(
			AuthenticationManager authenticationManager,
			UserAccountRepository userRepository,
			JwtTokenService jwtTokenService) {
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
		this.jwtTokenService = jwtTokenService;
	}

	@Override
	public LoginResponse login(LoginRequest request) {
		try {
			authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(
							request.username(), request.password()));
		} catch (AuthenticationException exception) {
			throw new InvalidCredentialsException();
		}

		UserAccount user = userRepository.findByUsername(request.username())
				.orElseThrow(InvalidCredentialsException::new);
		return jwtTokenService.issueToken(user);
	}
}
