package cm.enspy.xccm.security;

import cm.enspy.xccm.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service personnalisé réactif pour charger les détails de l'utilisateur.
 * Utilise UserRepository pour récupérer l'entité User de la base de données par email.
 */
@Service
@Primary
@RequiredArgsConstructor
public class CustomUserDetailsService implements ReactiveUserDetailsService {
    
    private final UserRepository userRepository;

    /**
     * Charge l'utilisateur par son identifiant (l'email) de manière réactive.
     * 
     * @param email L'email de l'utilisateur à charger.
     * @return Mono<UserDetails> contenant l'entité User.
     */
    @Override
    public Mono<UserDetails> findByUsername(String email) {
        // userRepository.findByEmail() doit retourner Mono<User>
        return userRepository.findByEmail(email)
                .cast(UserDetails.class)  // Cast User vers UserDetails
                .switchIfEmpty(Mono.error(
                    new UsernameNotFoundException("Utilisateur non trouvé avec l'email: " + email)
                ));
    }
}
