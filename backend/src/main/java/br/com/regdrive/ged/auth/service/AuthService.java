package br.com.regdrive.ged.auth.service;

import br.com.regdrive.ged.auth.dto.LoginRequest;
import br.com.regdrive.ged.auth.dto.LoginResponse;

public interface AuthService {

	LoginResponse login(LoginRequest request);
}
