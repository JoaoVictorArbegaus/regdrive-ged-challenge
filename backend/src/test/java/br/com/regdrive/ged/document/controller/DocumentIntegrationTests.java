package br.com.regdrive.ged.document.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.regdrive.ged.document.domain.Document;
import br.com.regdrive.ged.document.repository.DocumentRepository;
import br.com.regdrive.ged.user.domain.Role;
import br.com.regdrive.ged.user.domain.UserAccount;
import br.com.regdrive.ged.user.repository.UserAccountRepository;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private UserAccountRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	private UserAccount admin;
	private UserAccount user;

	@BeforeEach
	void setUpUsers() {
		documentRepository.deleteAll();
		userRepository.deleteAll();
		admin = userRepository.save(
				new UserAccount("admin", passwordEncoder.encode("admin123"), Role.ADMIN, "tenant-admin"));
		user = userRepository.save(
				new UserAccount("user", passwordEncoder.encode("user123"), Role.USER, "tenant-demo"));
		userRepository.save(
				new UserAccount("viewer", passwordEncoder.encode("viewer123"), Role.VIEWER, "tenant-demo"));
	}

	@AfterEach
	void cleanUp() {
		documentRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void userCreatesDraftWithAuthenticatedTenantAndOwner() throws Exception {
		String body = objectMapper.writeValueAsString(Map.of(
				"title", "Contrato",
				"description", "Contrato do cliente",
				"tags", Set.of("juridico", "cliente"),
				"tenantId", "forged-tenant",
				"ownerId", UUID.randomUUID()));

		mockMvc.perform(post("/api/documents")
					.header("Authorization", "Bearer " + loginToken("user", "user123"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/documents/.+")))
				.andExpect(jsonPath("$.title").value("Contrato"))
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.tenantId").value("tenant-demo"))
				.andExpect(jsonPath("$.ownerId").value(user.getId().toString()));
	}

	@Test
	void blankTitleReturnsValidationProblemDetails() throws Exception {
		mockMvc.perform(post("/api/documents")
					.header("Authorization", "Bearer " + loginToken("user", "user123"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"title\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.title").exists());
	}

	@Test
	void malformedBodyReturnsProblemDetails() throws Exception {
		mockMvc.perform(post("/api/documents")
					.header("Authorization", "Bearer " + loginToken("user", "user123"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{invalid-json}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
				.andExpect(jsonPath("$.path").value("/api/documents"));
	}

	@Test
	void viewerCannotCreateDocument() throws Exception {
		mockMvc.perform(post("/api/documents")
					.header("Authorization", "Bearer " + loginToken("viewer", "viewer123"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"title\":\"Contrato\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	void viewerFindsDocumentFromOwnTenant() throws Exception {
		Document document = documentRepository.save(
				new Document("Contrato", null, Set.of("juridico"), "tenant-demo", user.getId()));

		mockMvc.perform(get("/api/documents/{documentId}", document.getId())
					.header("Authorization", "Bearer " + loginToken("viewer", "viewer123")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(document.getId().toString()))
				.andExpect(jsonPath("$.tenantId").value("tenant-demo"));
	}

	@Test
	void userCannotFindDocumentFromAnotherTenant() throws Exception {
		Document document = documentRepository.save(
				new Document("Contrato", null, Set.of(), "tenant-admin", admin.getId()));

		mockMvc.perform(get("/api/documents/{documentId}", document.getId())
					.header("Authorization", "Bearer " + loginToken("user", "user123")))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
	}

	@Test
	void invalidDocumentIdReturnsProblemDetails() throws Exception {
		mockMvc.perform(get("/api/documents/not-a-uuid")
					.header("Authorization", "Bearer " + loginToken("user", "user123")))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
				.andExpect(jsonPath("$.parameter").value("documentId"));
	}

	private String loginToken(String username, String password) throws Exception {
		String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
		MvcResult result = mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString())
				.get("accessToken")
				.asText();
	}
}
