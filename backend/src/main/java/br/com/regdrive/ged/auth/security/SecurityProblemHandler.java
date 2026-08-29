package br.com.regdrive.ged.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public SecurityProblemHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		writeProblem(response, request, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Autenticação necessária");
	}

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			org.springframework.security.access.AccessDeniedException exception) throws IOException {
		writeProblem(response, request, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Acesso negado");
	}

	private void writeProblem(
			HttpServletResponse response,
			HttpServletRequest request,
			HttpStatus status,
			String code,
			String detail) throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(status.getReasonPhrase());
		problem.setProperty("code", code);
		problem.setProperty("path", request.getRequestURI());
		problem.setProperty("timestamp", Instant.now());

		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problem);
	}
}
