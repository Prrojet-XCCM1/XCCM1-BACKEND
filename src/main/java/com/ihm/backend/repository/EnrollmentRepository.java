package com.ihm.backend.repository;

import com.ihm.backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * Trouve un enrôlement spécifique pour un cours et un utilisateur
     * Utilise la navigation JPA: course.id et user.id
     */
    Optional<Enrollment> findByCourse_IdAndUser_Id(Integer courseId, UUID userId);

    /**
     * Trouve tous les enrôlements d'un utilisateur
     */
    List<Enrollment> findByUser_Id(UUID userId);

    /**
     * Trouve tous les enrôlements pour un cours donné
     */
    List<Enrollment> findByCourse_Id(Integer courseId);

    /**
     * Vérifie si un utilisateur est déjà enrôlé à un cours
     */
    boolean existsByCourse_IdAndUser_Id(Integer courseId, UUID userId);

    /**
     * Trouve les enrôlements d'un auteur filtrés par statut
     */
    List<Enrollment> findByCourse_Author_IdAndStatus(UUID authorId, com.ihm.backend.enums.EnrollmentStatus status);

    long countByStatus(com.ihm.backend.enums.EnrollmentStatus status);

    /**
     * Compte les enrollments par cours
     */
    long countByCourse_Id(Integer courseId);

    /**
     * Trouve les enrollments récents (7 derniers jours)
     */
    long countByEnrolledAtAfter(LocalDateTime date);

    /**
     * Calcule la progression moyenne
     */
    @Query("SELECT AVG(e.progress) FROM Enrollment e")
    Double calculateAverageProgress();

    /**
     * Calcule le taux de complétion
     */
    @Query("SELECT COUNT(e) * 100.0 / (SELECT COUNT(e2) FROM Enrollment e2) FROM Enrollment e WHERE e.completed = true")
    Double calculateCompletionRate();

    /**
     * Trouve les enrollments groupés par cours avec statistiques
     */
    @Query("SELECT e.course.id, e.course.title, COUNT(e), AVG(e.progress) FROM Enrollment e GROUP BY e.course.id, e.course.title")
    List<Object[]> findEnrollmentStatsByCourse();
}
