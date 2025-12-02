package cm.enspy.xccm.repository;

import cm.enspy.xccm.domain.entity.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import cm.enspy.xccm.domain.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public interface UserRepository extends R2dbcRepository<User, UUID> {
    Mono<User> findByEmail(String email);
    Mono<Boolean> existsByEmail(String email);

    /**
     * Trouve tous les utilisateurs par rôle.
     * Le type de retour est Flux<User> (0 à N résultats).
     *
     * @param role Le rôle à filtrer
     * @return Flux des utilisateurs avec ce rôle
     */
    Flux<User> findByRole(UserRole role);

    /**
     * Trouve tous les utilisateurs actifs.
     *
     * @param isActive Le statut actif
     * @return Flux des utilisateurs actifs
     */
    Flux<User> findByIsActive(Boolean isActive);

    /**
     * Trouve tous les utilisateurs vérifiés.
     *
     * @param isVerified Le statut de vérification
     * @return Flux des utilisateurs vérifiés
     */
    Flux<User> findByIsVerified(Boolean isVerified);

    /**
     * Trouve les utilisateurs créés après une certaine date.
     *
     * @param date La date de référence
     * @return Flux des utilisateurs
     */
    Flux<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Recherche d'utilisateurs par nom (case insensitive).
     *
     * @param firstName Le nom à rechercher
     * @return Flux des utilisateurs correspondants
     */
    Flux<User> findByFirstNameContainingIgnoreCase(String firstname);

    /**
     * Compte le nombre d'utilisateurs par rôle.
     *
     * @param role Le rôle
     * @return Mono contenant le nombre d'utilisateurs
     */
    Mono<Long> countByRole(UserRole role);

    /**
     * Trouve les utilisateurs actifs par rôle.
     *
     * @param role Le rôle
     * @param isActive Le statut actif
     * @return Flux des utilisateurs
     */
    Flux<User> findByRoleAndIsActive(UserRole role, Boolean isActive);

    /**
     * Trouve les utilisateurs actifs récemment.
     * Les requêtes personnalisées doivent être adaptées ou peuvent être implémentées
     * dans un service utilisant ReactiveDatabaseClient ou en utilisant l'approche Query By Example.
     * Ici, nous gardons les requêtes dérivées du nom de méthode.
     *
     * @param since Date de référence
     * @return Flux des utilisateurs actifs récemment
     */
    Flux<User> findByLastLoginAtAfterOrderByLastLoginAtDesc(LocalDateTime since);

    /**
     * Supprime les utilisateurs non vérifiés créés avant une certaine date.
     * (Pour nettoyer les comptes non activés)
     *
     * @param date Date limite
     * @return Mono<Void> pour indiquer la fin de l'opération
     */
    Mono<Void> deleteByIsVerifiedFalseAndCreatedAtBefore(LocalDateTime date);

    @Query("UPDATE users SET last_login = NOW() WHERE id = :userid")
    Mono<Void> updateLastLogin(UUID userId);
}

