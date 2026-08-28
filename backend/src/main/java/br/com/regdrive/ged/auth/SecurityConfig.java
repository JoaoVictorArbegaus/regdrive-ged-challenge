package br.com.regdrive.ged.auth;

import br.com.regdrive.ged.user.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			SecurityProblemHandler problemHandler) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/health").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/**").hasAnyRole("ADMIN", "USER")
						.requestMatchers(HttpMethod.PUT, "/api/**").hasAnyRole("ADMIN", "USER")
						.requestMatchers(HttpMethod.PATCH, "/api/**").hasAnyRole("ADMIN", "USER")
						.requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN", "USER")
						.requestMatchers("/api/**").authenticated()
						.anyRequest().permitAll())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(problemHandler)
						.accessDeniedHandler(problemHandler))
				.oauth2ResourceServer(resourceServer -> resourceServer
						.authenticationEntryPoint(problemHandler)
						.accessDeniedHandler(problemHandler)
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
		return http.build();
	}

	@Bean
	UserDetailsService userDetailsService(UserAccountRepository userRepository) {
		return username -> userRepository.findByUsername(username)
				.map(user -> User.withUsername(user.getUsername())
						.password(user.getPasswordHash())
						.roles(user.getRole().name())
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	JwtEncoder jwtEncoder(@Value("${app.security.jwt.secret}") String secret) {
		return NimbusJwtEncoder.withSecretKey(secretKey(secret)).build();
	}

	@Bean
	JwtDecoder jwtDecoder(@Value("${app.security.jwt.secret}") String secret) {
		return NimbusJwtDecoder.withSecretKey(secretKey(secret))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
	}

	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt ->
				List.of(new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString("role"))));
		return converter;
	}

	private SecretKey secretKey(String secret) {
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}
}
