
package cm.enspy.xccm.repository;

import cm.enspy.xccm.domain.entity.Section;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface SectionRepository extends ReactiveCrudRepository<Section, UUID> {

    // Trouver toutes les sections d'une composition (triées par ordre)
    Flux<Section> findByCompositionIdOrderByOrderIndexAsc(UUID compositionId);

    // Compter les sections d'une composition
    Mono<Long> countByCompositionId(UUID compositionId);

    // Trouver la section avec l'ordre maximum
    @Query("SELECT MAX(order_index) FROM sections WHERE composition_id = :compositionId")
    Mono<Integer> findMaxOrderIndex(UUID compositionId);

    // Supprimer toutes les sections d'une composition
    Mono<Void> deleteByCompositionId(UUID compositionId);
}