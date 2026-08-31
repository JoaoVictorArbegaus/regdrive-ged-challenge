package br.com.regdrive.ged.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.regdrive.ged.auth.dto.LoginRequest;
import br.com.regdrive.ged.auth.dto.LoginResponse;
import br.com.regdrive.ged.auth.security.JwtTokenService;
import br.com.regdrive.ged.user.domain.Role;
import br.com.regdrive.ged.user.domain.UserAccount;
import br.com.regdrive.ged.user.repository.UserAccountRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private UserAccountRepository userRepository;

	@Mock
	private JwtTokenService jwtTokenService;

	@InjectMocks
	private AuthApplicationService authService;

	@Test
	void loginAuthenticatesUserAndReturnsIssuedToken() {
		LoginRequest request = new LoginRequest("user", "user123");
		UserAccount user = new UserAccount("user", "hash", Role.USER, "tenant-demo");
		LoginResponse expected = new LoginResponse("token", "Bearer", Instant.now().plusSeconds(900));
		when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
		when(jwtTokenService.issueToken(user)).thenReturn(expected);

		LoginResponse response = authService.login(request);

		assertThat(response).isEqualTo(expected);
		verify(authenticationManager).authenticate(any());
	}
}
