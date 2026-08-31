package br.com.regdrive.ged.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 100)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(name = "tenant_id", nullable = false, length = 100)
	private String tenantId;

	public UserAccount(String username, String passwordHash, Role role, String tenantId) {
		this.id = UUID.randomUUID();
		this.username = username;
		this.passwordHash = passwordHash;
		this.role = role;
		this.tenantId = tenantId;
	}

}
