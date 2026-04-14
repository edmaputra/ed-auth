package io.github.edmaputra.enhauthserv.repository;

import io.github.edmaputra.enhauthserv.entity.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

  Optional<UserProfile> findByUsername(String username);
}