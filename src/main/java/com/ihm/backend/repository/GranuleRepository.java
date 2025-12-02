
package cm.enspy.xccm.repository;

import cm.enspy.xccm.domain.entity.Granule;
import cm.enspy.xccm.domain.enums.GranuleType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface GranuleRepository extends ReactiveCrudRepository<Granule, UUID> {

    // Trouver tous les granules d'une section (triés par ordre)
    Flux<Granule> findBySectionIdOrderByOrderIndexAsc(UUID sectionId);

    // Trouver les granules réutilisables
    Flux<Granule> findByReusableTrue();

    // Recherche par titre
    @Query("SELECT * FROM granules WHERE LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Flux<Granule> searchByTitle(String keyword);

    // Recherche par tags (si stocké en String)
    @Query("SELECT * FROM granules WHERE LOWER(tags) LIKE LOWER(CONCAT('%', :tag, '%'))")
    Flux<Granule> findByTag(String tag);

    // Trouver par type
    Flux<Granule> findByType(GranuleType type);

    // Compter les granules d'une section
    Mono<Long> countBySectionId(UUID sectionId);

    // Trouver l'ordre maximum dans une section
    @Query("SELECT MAX(order_index) FROM granules WHERE section_id = :sectionId")
    Mono<Integer> findMaxOrderIndex(UUID sectionId);

    // Supprimer tous les granules d'une section
    Mono<Void> deleteAllBySectionId(UUID sectionId);
}