package io.github.edmaputra.enhauthserv.users;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

  Optional<UserProfile> findByUsername(String username);

  Optional<UserProfile> findByTenantAndUsername(String tenant, String username);
}
