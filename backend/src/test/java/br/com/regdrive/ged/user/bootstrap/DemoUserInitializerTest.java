package br.com.regdrive.ged.user.bootstrap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.regdrive.ged.user.domain.UserAccount;
import br.com.regdrive.ged.user.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DemoUserInitializerTest {

	@Mock
	private UserAccountRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Test
	void repeatedInitializationDoesNotDuplicateUsers() throws Exception {
		when(userRepository.existsByUsername(any()))
				.thenReturn(false, false, false, true, true, true);
		when(passwordEncoder.encode(any())).thenReturn("bcrypt-hash");
		DemoUserInitializer initializer = new DemoUserInitializer(
				userRepository, passwordEncoder, "admin123", "user123", "viewer123");

		initializer.run(null);
		initializer.run(null);

		verify(userRepository, times(3)).save(any(UserAccount.class));
	}
}
