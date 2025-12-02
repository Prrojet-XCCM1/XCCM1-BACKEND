package cm.enspy.xccm.repository;

import cm.enspy.xccm.domain.entity.CourseEnrollment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Repository pour l'entité CourseEnrollment, utilisant R2DBC et les types réactifs (Mono, Flux).
 * Fournit les opérations CRUD et les requêtes personnalisées pour le contexte réactif.
 */
@Repository
public interface CourseEnrollmentRepository extends ReactiveCrudRepository<CourseEnrollment, UUID> {

    /**
     * Trouve les inscriptions d'un étudiant.
     *
     * @param studentId L'ID de l'étudiant
     * @return Flux des inscriptions de l'étudiant
     */
    Flux<CourseEnrollment> findByStudentId(UUID studentId);

    /**
     * Trouve les inscriptions d'un cours.
     *
     * @param courseId L'ID du cours
     * @return Flux des inscriptions du cours
     */
    Flux<CourseEnrollment> findByCourseId(UUID courseId);

    /**
     * Trouve une inscription par étudiant et cours.
     *
     * @param studentId L'ID de l'étudiant
     * @param courseId L'ID du cours
     * @return Mono contenant l'inscription ou vide
     */
    Mono<CourseEnrollment> findByStudentIdAndCourseId(UUID studentId, UUID courseId);

    /**
     * Trouve les inscriptions par statut.
     *
     * @param status Le statut de l'inscription
     * @return Flux des inscriptions avec ce statut
     */
    Flux<CourseEnrollment> findByStatus(CourseEnrollment.EnrollmentStatus status);

    /**
     * Compte le nombre d'inscriptions par cours.
     *
     * @param courseId L'ID du cours
     * @return Mono contenant le nombre d'inscriptions
     */
    Mono<Long> countByCourseId(UUID courseId);

    /**
     * Supprime les inscriptions d'un étudiant.
     *
     * @param studentId L'ID de l'étudiant
     * @return Mono<Void> pour indiquer la fin de l'opération
     */
    Mono<Void> deleteByStudentId(UUID studentId);
}