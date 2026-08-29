package br.com.regdrive.ged.user.repository;

import br.com.regdrive.ged.user.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

	Optional<UserAccount> findByUsername(String username);

	boolean existsByUsername(String username);
}
