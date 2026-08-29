package br.com.regdrive.ged.auth.controller;

import br.com.regdrive.ged.auth.api.AuthApi;
import br.com.regdrive.ged.auth.dto.LoginRequest;
import br.com.regdrive.ged.auth.dto.LoginResponse;
import br.com.regdrive.ged.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public ResponseEntity<LoginResponse> login(LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}
}
