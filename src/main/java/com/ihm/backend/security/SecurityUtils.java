package cm.enspy.xccm.security;

import cm.enspy.xccm.domain.entity.User; // Assurez-vous que c'est le bon chemin pour votre entité User
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Classe utilitaire pour les opérations de sécurité réactives (WebFlux).
 * Utilise ReactiveSecurityContextHolder pour un accès non-bloquant.
 */
@Slf4j
public final class SecurityUtils {

    private SecurityUtils() {
        // Constructeur privé pour classe utilitaire
    }
    
    /**
     * Récupère de manière réactive l'objet Authentication depuis le contexte de sécurité.
     * Filtre les authentifications nulles, non authentifiées ou anonymes.
     *
     * @return Mono contenant l'objet Authentication, ou Mono.empty() si non authentifié.
     */
    private static Mono<Authentication> getReactiveAuthentication() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken));
    }

    /**
     * Récupère l'utilisateur actuellement authentifié (réactif)
     * * @return Mono contenant l'utilisateur ou Mono.empty() si non authentifié
     */
    public static Mono<User> getCurrentUserReactive() {
        return getReactiveAuthentication()
                .filter(authentication -> authentication.getPrincipal() instanceof User)
                .map(authentication -> (User) authentication.getPrincipal());
    }

    /**
     * Récupère l'email de l'utilisateur actuellement authentifié (réactif)
     * * @return Mono contenant l'email ou Mono.empty() si non authentifié
     */
    public static Mono<String> getCurrentUserEmailReactive() {
        return getCurrentUserReactive().map(User::getEmail);
    }

    /**
     * Récupère l'ID de l'utilisateur actuellement authentifié (réactif)
     * * @return Mono contenant l'UUID ou Mono.empty() si non authentifié
     */
    // Renommé pour WebFlux
    public static Mono<UUID> getCurrentUserIdReactive() {
        return getCurrentUserReactive().map(User::getId);
    }

    /**
     * Vérifie si un utilisateur est actuellement authentifié (réactif)
     * * @return Mono<Boolean> true si un utilisateur est authentifié
     */
    public static Mono<Boolean> isAuthenticatedReactive() {
        return getReactiveAuthentication()
                .hasElement(); // Vrai si le Mono<Authentication> a été trouvé
    }

    /**
     * Vérifie si l'utilisateur actuel a un rôle spécifique (réactif)
     * * @param role Le rôle à vérifier (ex: "TEACHER", "STUDENT", "ADMIN")
     * @return Mono<Boolean> true si l'utilisateur a le rôle
     */
    public static Mono<Boolean> hasRoleReactive(String role) {
        final String roleName = "ROLE_" + role;
        
        return getReactiveAuthentication()
                .map(authentication -> authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(roleName))
                )
                .defaultIfEmpty(false);
    }

    /**
     * Vérifie si l'utilisateur actuel est un enseignant (réactif)
     * * @return Mono<Boolean> true si l'utilisateur est un enseignant
     */
    public static Mono<Boolean> isTeacherReactive() {
        return hasRoleReactive("TEACHER");
    }

    /**
     * Vérifie si l'utilisateur actuel est un étudiant (réactif)
     * * @return Mono<Boolean> true si l'utilisateur est un étudiant
     */
    public static Mono<Boolean> isStudentReactive() {
        return hasRoleReactive("STUDENT");
    }

    /**
     * Vérifie si l'utilisateur actuel est un administrateur (réactif)
     * * @return Mono<Boolean> true si l'utilisateur est un administrateur
     */
    public static Mono<Boolean> isAdminReactive() {
        return hasRoleReactive("ADMIN");
    }

    /**
     * Vérifie si l'utilisateur actuel est le propriétaire d'une ressource ou un admin (réactif)
     * * @param resourceOwnerId L'ID du propriétaire de la ressource
     * @return Mono<Boolean> true si l'utilisateur est le propriétaire ou un admin
     */
    public static Mono<Boolean> isOwnerOrAdminReactive(UUID resourceOwnerId) {
        // Exécuter les vérifications d'isAdmin et getCurrentUserId en parallèle/séquence non bloquante
        return isAdminReactive()
                .flatMap(isAdmin -> {
                    if (isAdmin) {
                        return Mono.just(true);
                    }
                    return getCurrentUserIdReactive()
                            .map(userId -> userId.equals(resourceOwnerId))
                            .defaultIfEmpty(false);
                });
    }
    
    // --- Les autres méthodes n'ont plus d'utilité directe car elles utilisaient l'API synchrone ---
    // (getCurrentUserFullName, getCurrentUserDetails, canAccessResource, logCurrentUser)
    // Elles peuvent être refaites à la demande si elles sont nécessaires dans le contexte réactif.
}