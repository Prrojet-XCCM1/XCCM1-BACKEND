// UserRepository.java
package com.ihm.backend.repository;

import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Méthode pour récupérer les utilisateurs par rôle avec pagination
    List<User> findByRole(UserRole role);

    long countByRole(UserRole role);

    // Vous pouvez aussi ajouter des méthodes spécifiques si besoin
    Optional<User> findByIdAndRole(UUID id, UserRole role);
}