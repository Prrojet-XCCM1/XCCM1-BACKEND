// UserRepository.java
package com.ihm.backend.repository;

import com.ihm.backend.domain.entity.User;
import com.ihm.backend.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByRole(UserRole role);

    List<User> findAllByActiveAndVerified(boolean active, boolean verified);
}