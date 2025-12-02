
package cm.enspy.xccm.repository;

import cm.enspy.xccm.domain.entity.Composition;
import cm.enspy.xccm.domain.enums.CompositionStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface CompositionRepository extends ReactiveCrudRepository<Composition, UUID> {

    // Trouver toutes les compositions d'un enseignant
    Flux<Composition> findByTeacherId(UUID teacherId);

    // Trouver par statut
    Flux<Composition> findByStatus(CompositionStatus status);

    // Trouver les compositions publiées d'un enseignant
    Flux<Composition> findByTeacherIdAndStatus(UUID teacherId, CompositionStatus status);

    // Recherche par titre (contient)
    @Query("SELECT * FROM compositions WHERE LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Flux<Composition> searchByTitle(String keyword);

    // Compter les compositions d'un enseignant
    Mono<Long> countByTeacherId(UUID teacherId);
}
