package br.com.regdrive.ged.shared.error;

import br.com.regdrive.ged.auth.exception.InvalidCredentialsException;
import br.com.regdrive.ged.document.exception.DocumentNotFoundException;
import br.com.regdrive.ged.document.exception.OwnerNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ProblemDetail> handleMalformedRequest(
			HttpMessageNotReadableException exception, HttpServletRequest request) {
		ProblemDetail problem = createProblem(
				HttpStatus.BAD_REQUEST,
				"MALFORMED_REQUEST",
				"Requisição inválida",
				"O corpo da requisição não pôde ser interpretado.",
				request);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ProblemDetail> handleInvalidParameter(
			MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
		ProblemDetail problem = createProblem(
				HttpStatus.BAD_REQUEST,
				"INVALID_PARAMETER",
				"Parâmetro inválido",
				"O parâmetro informado possui formato inválido.",
				request);
		problem.setProperty("parameter", exception.getName());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
	}

	@ExceptionHandler(DocumentNotFoundException.class)
	ResponseEntity<ProblemDetail> handleDocumentNotFound(
			DocumentNotFoundException exception, HttpServletRequest request) {
		ProblemDetail problem = createProblem(
				HttpStatus.NOT_FOUND,
				"DOCUMENT_NOT_FOUND",
				"Documento não encontrado",
				exception.getMessage(),
				request);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}

	@ExceptionHandler(OwnerNotFoundException.class)
	ResponseEntity<ProblemDetail> handleOwnerNotFound(
			OwnerNotFoundException exception, HttpServletRequest request) {
		ProblemDetail problem = createProblem(
				HttpStatus.BAD_REQUEST,
				"OWNER_NOT_FOUND",
				"Responsável não encontrado",
				exception.getMessage(),
				request);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ProblemDetail> handleAccessDenied(
			AccessDeniedException exception, HttpServletRequest request) {
		ProblemDetail problem = createProblem(
				HttpStatus.FORBIDDEN,
				"ACCESS_DENIED",
				"Acesso negado",
				exception.getMessage(),
				request);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ResponseEntity<ProblemDetail> handleInvalidCredentials(
			InvalidCredentialsException exception, HttpServletRequest request) {
		ProblemDetail problem = createProblem(
				HttpStatus.UNAUTHORIZED,
				"INVALID_CREDENTIALS",
				"Credenciais inválidas",
				exception.getMessage(),
				request);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ProblemDetail> handleResourceNotFound(
			NoResourceFoundException exception, HttpServletRequest request) {
		ProblemDetail problem = createProblem(
				HttpStatus.NOT_FOUND,
				"RESOURCE_NOT_FOUND",
				"Recurso não encontrado",
				exception.getMessage(),
				request);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> handleValidation(
			MethodArgumentNotValidException exception, HttpServletRequest request) {
		ProblemDetail problem = createProblem(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_ERROR",
				"Dados inválidos",
				"Um ou mais campos informados são inválidos.",
				request);

		Map<String, String> fieldErrors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors().forEach(error ->
				fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		problem.setProperty("fieldErrors", fieldErrors);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
	}

	private ProblemDetail createProblem(
			HttpStatus status,
			String code,
			String title,
			String detail,
			HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setProperty("code", code);
		problem.setProperty("path", request.getRequestURI());
		problem.setProperty("timestamp", Instant.now());
		return problem;
	}
}
