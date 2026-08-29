package br.com.regdrive.ged.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.regdrive.ged.user.domain.Role;
import br.com.regdrive.ged.user.domain.UserAccount;
import br.com.regdrive.ged.user.repository.UserAccountRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserAccountRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUpUsers() {
		userRepository.deleteAll();
		userRepository.save(new UserAccount("admin", passwordEncoder.encode("admin123"), Role.ADMIN, "tenant-admin"));
		userRepository.save(new UserAccount("user", passwordEncoder.encode("user123"), Role.USER, "tenant-demo"));
		userRepository.save(new UserAccount("viewer", passwordEncoder.encode("viewer123"), Role.VIEWER, "tenant-demo"));
	}

	@ParameterizedTest
	@CsvSource({
			"admin, admin123, ADMIN, tenant-admin",
			"user, user123, USER, tenant-demo",
			"viewer, viewer123, VIEWER, tenant-demo"
	})
	void loginReturnsTokenWithUserClaims(
			String username, String password, String role, String tenantId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(loginBody(username, password)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresAt").exists())
				.andReturn();

		String token = objectMapper.readTree(result.getResponse().getContentAsString())
				.get("accessToken")
				.asText();
		Jwt jwt = jwtDecoder.decode(token);

		assertThat(jwt.getSubject()).isEqualTo(username);
		assertThat(jwt.getClaimAsString("role")).isEqualTo(role);
		assertThat(jwt.getClaimAsString("tenant_id")).isEqualTo(tenantId);
		assertThat(jwt.getClaimAsString("user_id")).isNotBlank();
		assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
	}

	@Test
	void invalidCredentialsReturnProblemDetails() throws Exception {
		mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(loginBody("user", "wrong-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.path").value("/api/auth/login"));
	}

	@Test
	void blankUsernameReturnsValidationProblemDetails() throws Exception {
		mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(loginBody("", "user123")))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.username").exists());
	}

	@Test
	void protectedRouteWithoutTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/missing"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void invalidTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/missing")
					.header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void viewerCannotWrite() throws Exception {
		mockMvc.perform(post("/api/missing")
					.header("Authorization", "Bearer " + loginToken("viewer", "viewer123")))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	private String loginToken(String username, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(loginBody(username, password)))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString())
				.get("accessToken")
				.asText();
	}

	private String loginBody(String username, String password) {
		return """
				{"username":"%s","password":"%s"}
				""".formatted(username, password);
	}
}
