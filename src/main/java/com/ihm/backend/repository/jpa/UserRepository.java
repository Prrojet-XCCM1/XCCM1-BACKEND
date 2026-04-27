// UserRepository.java
package com.ihm.backend.repository.jpa;

import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByMoodleSub(String moodleSub);

    boolean existsByEmail(String email);

    List<User> findAllByRole(UserRole role);

    List<User> findAllByActiveAndVerified(boolean active, boolean verified);

    // Méthode pour récupérer les utilisateurs par rôle avec pagination
    List<User> findByRole(UserRole role);

    long countByRole(UserRole role);

    java.util.List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    org.springframework.data.domain.Page<User> searchUsers(@org.springframework.data.repository.query.Param("query") String query, org.springframework.data.domain.Pageable pageable);

    // Vous pouvez aussi ajouter des méthodes spécifiques si besoin
    Optional<User> findByIdAndRole(UUID id, UserRole role);
}