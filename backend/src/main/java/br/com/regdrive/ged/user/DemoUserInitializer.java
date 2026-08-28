package br.com.regdrive.ged.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "docker"})
@ConditionalOnProperty(name = "app.demo-users.enabled", havingValue = "true")
public class DemoUserInitializer implements ApplicationRunner {

	private final UserAccountRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final String adminPassword;
	private final String userPassword;
	private final String viewerPassword;

	public DemoUserInitializer(
			UserAccountRepository userRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.demo-users.admin-password}") String adminPassword,
			@Value("${app.demo-users.user-password}") String userPassword,
			@Value("${app.demo-users.viewer-password}") String viewerPassword) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.adminPassword = adminPassword;
		this.userPassword = userPassword;
		this.viewerPassword = viewerPassword;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		createIfMissing("admin", adminPassword, Role.ADMIN, "tenant-admin");
		createIfMissing("user", userPassword, Role.USER, "tenant-demo");
		createIfMissing("viewer", viewerPassword, Role.VIEWER, "tenant-demo");
	}

	private void createIfMissing(String username, String password, Role role, String tenantId) {
		if (!userRepository.existsByUsername(username)) {
			userRepository.save(new UserAccount(username, passwordEncoder.encode(password), role, tenantId));
		}
	}
}
