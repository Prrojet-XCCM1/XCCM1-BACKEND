package cm.enspy.xccm.repository;

import cm.enspy.xccm.domain.entity.Course;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repository pour l'entité Course, utilisant R2DBC et les types réactifs (Mono, Flux).
 * Fournit les opérations CRUD et les requêtes personnalisées pour le contexte réactif.
 */
@Repository
public interface CourseRepository extends ReactiveCrudRepository<Course, UUID> {

    /**
     * Trouve un cours par son titre.
     *
     * @param title Le titre du cours
     * @return Mono contenant le cours ou vide
     */
    Mono<Course> findByTitle(String title);

    /**
     * Trouve les cours par catégorie.
     *
     * @param category La catégorie du cours
     * @return Flux des cours avec cette catégorie
     */
    Flux<Course> findByCategory(String category);

    /**
     * Trouve les cours publiés après une certaine date.
     *
     * @param date La date de référence
     * @return Flux des cours publiés après cette date
     */
    Flux<Course> findByDatePublicationAfter(LocalDateTime date);

    /**
     * Trouve les cours de l'auteur.
     *
     * @param authorId L'ID de l'auteur
     * @return Flux des cours de l'auteur
     */
    Flux<Course> findByAuthorId(UUID authorId);

    /**
     * Compte le nombre de cours par catégorie.
     *
     * @param category La catégorie du cours
     * @return Mono contenant le nombre de cours
     */
    Mono<Long> countByCategory(String category);

    /**
     * Supprime les cours qui n'ont pas été publiés.
     *
     * @return Mono<Void> pour indiquer la fin de l'opération
     */
    Mono<Void> deleteByDatePublicationIsNull();
}