package br.com.regdrive.ged.document.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.regdrive.ged.document.domain.Document;
import br.com.regdrive.ged.document.domain.DocumentStatus;
import br.com.regdrive.ged.document.repository.DocumentRepository;
import br.com.regdrive.ged.user.domain.Role;
import br.com.regdrive.ged.user.domain.UserAccount;
import br.com.regdrive.ged.user.repository.UserAccountRepository;
import java.time.Instant;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
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

	@Test
	void userUploadsFirstDocumentVersion() throws Exception {
		Document document = documentRepository.save(
				new Document("Contrato", null, Set.of(), "tenant-demo", user.getId()));
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"contrato.pdf",
				"application/pdf",
				"%PDF-1.4\ncontent".getBytes(java.nio.charset.StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/documents/{documentId}/versions", document.getId())
					.file(file)
					.header("Authorization", "Bearer " + loginToken("user", "user123")))
				.andExpect(status().isCreated())
				.andExpect(header().string(
						"Location",
						"/api/documents/" + document.getId() + "/versions/1"))
				.andExpect(jsonPath("$.documentId").value(document.getId().toString()))
				.andExpect(jsonPath("$.versionNumber").value(1))
				.andExpect(jsonPath("$.originalFilename").value("contrato.pdf"))
				.andExpect(jsonPath("$.checksum")
						.value("4fbf22661c0285c55f8fa7518f954910e8b0c33750658fc6863f66aa6b94c7fd"))
				.andExpect(jsonPath("$.fileKey").doesNotExist());
	}

	@Test
	void uploadRejectsInvalidFileSignature() throws Exception {
		Document document = documentRepository.save(
				new Document("Contrato", null, Set.of(), "tenant-demo", user.getId()));
		MockMultipartFile file = new MockMultipartFile(
				"file", "contrato.pdf", "application/pdf", "invalid".getBytes());

		mockMvc.perform(multipart("/api/documents/{documentId}/versions", document.getId())
					.file(file)
					.header("Authorization", "Bearer " + loginToken("user", "user123")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_FILE"));
	}

	@Test
	void userListsOwnTenantWithCombinedFilters() throws Exception {
		Document matching = new Document(
				"Contrato Especial", null, Set.of("juridico"), "tenant-demo", user.getId());
		matching.transitionTo(DocumentStatus.PUBLISHED);
		matching = documentRepository.save(matching);
		documentRepository.save(
				new Document("Contrato Comum", null, Set.of("juridico"), "tenant-demo", user.getId()));
		documentRepository.save(
				new Document("Contrato Especial", null, Set.of("juridico"), "tenant-admin", admin.getId()));
		Instant createdAt = matching.getCreatedAt();

		mockMvc.perform(get("/api/documents")
					.header("Authorization", "Bearer " + loginToken("user", "user123"))
					.param("tenantId", "tenant-admin")
					.param("ownerId", user.getId().toString())
					.param("title", "especial")
					.param("tag", "juridico")
					.param("status", "PUBLISHED")
					.param("createdFrom", createdAt.minusSeconds(1).toString())
					.param("createdTo", createdAt.plusSeconds(1).toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].id").value(matching.getId().toString()))
				.andExpect(jsonPath("$.content[0].tenantId").value("tenant-demo"))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void adminListsAllTenantsOrFiltersOneTenant() throws Exception {
		documentRepository.save(
				new Document("Documento Demo", null, Set.of(), "tenant-demo", user.getId()));
		documentRepository.save(
				new Document("Documento Admin", null, Set.of(), "tenant-admin", admin.getId()));
		String token = loginToken("admin", "admin123");

		mockMvc.perform(get("/api/documents")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2));

		mockMvc.perform(get("/api/documents")
					.header("Authorization", "Bearer " + token)
					.param("tenantId", "tenant-demo"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].tenantId").value("tenant-demo"));
	}

	@Test
	void listAppliesPaginationAndSorting() throws Exception {
		documentRepository.save(new Document("Gama", null, Set.of(), "tenant-demo", user.getId()));
		documentRepository.save(new Document("Alfa", null, Set.of(), "tenant-demo", user.getId()));
		documentRepository.save(new Document("Beta", null, Set.of(), "tenant-demo", user.getId()));

		mockMvc.perform(get("/api/documents")
					.header("Authorization", "Bearer " + loginToken("user", "user123"))
					.param("page", "0")
					.param("size", "2")
					.param("sort", "title,asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.content[0].title").value("Alfa"))
				.andExpect(jsonPath("$.content[1].title").value("Beta"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void listRejectsInvalidParameters() throws Exception {
		String token = loginToken("user", "user123");

		mockMvc.perform(get("/api/documents")
					.header("Authorization", "Bearer " + token)
					.param("page", "-1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
				.andExpect(jsonPath("$.parameter").value("page"));

		mockMvc.perform(get("/api/documents")
					.header("Authorization", "Bearer " + token)
					.param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
				.andExpect(jsonPath("$.parameter").value("size"));

		mockMvc.perform(get("/api/documents")
					.header("Authorization", "Bearer " + token)
					.param("sort", "description,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
				.andExpect(jsonPath("$.parameter").value("sort"));
	}

	@Test
	void putReplacesMetadataAndPersistsChanges() throws Exception {
		Document document = documentRepository.save(
				new Document("Contrato", "Descrição", Set.of("antiga"), "tenant-demo", user.getId()));
		String token = loginToken("user", "user123");
		String body = objectMapper.writeValueAsString(Map.of(
				"title", "Contrato atualizado",
				"description", "Nova descrição",
				"tags", Set.of("nova", "vigente")));

		mockMvc.perform(put("/api/documents/{documentId}", document.getId())
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Contrato atualizado"))
				.andExpect(jsonPath("$.description").value("Nova descrição"))
				.andExpect(jsonPath("$.tags").isArray())
				.andExpect(jsonPath("$.tags.length()").value(2));

		mockMvc.perform(get("/api/documents/{documentId}", document.getId())
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Contrato atualizado"))
				.andExpect(jsonPath("$.description").value("Nova descrição"))
				.andExpect(jsonPath("$.tags.length()").value(2));
	}

	@Test
	void patchTransitionsDocumentUntilArchived() throws Exception {
		Document document = documentRepository.save(
				new Document("Contrato", null, Set.of(), "tenant-demo", user.getId()));
		String token = loginToken("user", "user123");

		changeStatus(document.getId(), "PUBLISHED", token)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PUBLISHED"));
		changeStatus(document.getId(), "ARCHIVED", token)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ARCHIVED"));
	}

	@Test
	void archivedDocumentRejectsMetadataUpdate() throws Exception {
		Document document = new Document("Contrato", null, Set.of(), "tenant-demo", user.getId());
		document.transitionTo(DocumentStatus.PUBLISHED);
		document.transitionTo(DocumentStatus.ARCHIVED);
		documentRepository.save(document);

		mockMvc.perform(put("/api/documents/{documentId}", document.getId())
					.header("Authorization", "Bearer " + loginToken("user", "user123"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"title\":\"Outro\",\"tags\":[]}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DOCUMENT_ARCHIVED"));
	}

	@Test
	void invalidStatusTransitionReturnsConflict() throws Exception {
		Document document = documentRepository.save(
				new Document("Contrato", null, Set.of(), "tenant-demo", user.getId()));

		changeStatus(document.getId(), "ARCHIVED", loginToken("user", "user123"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
	}

	@Test
	void userCannotUpdateDocumentFromAnotherTenant() throws Exception {
		Document document = documentRepository.save(
				new Document("Contrato", null, Set.of(), "tenant-admin", admin.getId()));

		mockMvc.perform(put("/api/documents/{documentId}", document.getId())
					.header("Authorization", "Bearer " + loginToken("user", "user123"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"title\":\"Outro\",\"tags\":[]}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
	}

	@Test
	void viewerCannotChangeDocumentStatus() throws Exception {
		Document document = documentRepository.save(
				new Document("Contrato", null, Set.of(), "tenant-demo", user.getId()));

		changeStatus(document.getId(), "PUBLISHED", loginToken("viewer", "viewer123"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	void nullStatusReturnsValidationProblemDetails() throws Exception {
		Document document = documentRepository.save(
				new Document("Contrato", null, Set.of(), "tenant-demo", user.getId()));

		mockMvc.perform(patch("/api/documents/{documentId}/status", document.getId())
					.header("Authorization", "Bearer " + loginToken("user", "user123"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.status").exists());
	}

	private ResultActions changeStatus(
			UUID documentId, String statusValue, String token) throws Exception {
		return mockMvc.perform(patch("/api/documents/{documentId}/status", documentId)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("status", statusValue))));
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
