package com.ihm.backend.repository.jpa;

import com.ihm.backend.entity.ClassEnrollment;
import com.ihm.backend.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    /**
     * Tous les inscrits à une classe
     */
    List<ClassEnrollment> findByCourseClass_Id(Long classId);

    /**
     * Inscriptions d'un étudiant
     */
    List<ClassEnrollment> findByStudent_Id(UUID studentId);

    /**
     * Inscription spécifique étudiant/classe
     */
    Optional<ClassEnrollment> findByCourseClass_IdAndStudent_Id(Long classId, UUID studentId);

    /**
     * Vérifier si un étudiant est déjà inscrit à une classe
     */
    boolean existsByCourseClass_IdAndStudent_Id(Long classId, UUID studentId);

    /**
     * Toutes les demandes en attente pour les classes d'un enseignant
     */
    List<ClassEnrollment> findByCourseClass_Teacher_IdAndStatus(UUID teacherId, EnrollmentStatus status);

    /**
     * Inscrits d'une classe filtrés par statut
     */
    List<ClassEnrollment> findByCourseClass_IdAndStatus(Long classId, EnrollmentStatus status);

    /**
     * Nombre d'inscrits approuvés dans une classe
     */
    long countByCourseClass_IdAndStatus(Long classId, EnrollmentStatus status);

    /**
     * Nombre total d'inscriptions d'un enseignant (toutes classes confondues)
     */
    @Query("SELECT COUNT(e) FROM ClassEnrollment e WHERE e.courseClass.teacher.id = :teacherId")
    long countByTeacherId(UUID teacherId);

    /**
     * Nombre d'inscriptions en attente pour un enseignant
     */
    @Query("SELECT COUNT(e) FROM ClassEnrollment e WHERE e.courseClass.teacher.id = :teacherId AND e.status = 'PENDING'")
    long countPendingByTeacherId(UUID teacherId);
}
