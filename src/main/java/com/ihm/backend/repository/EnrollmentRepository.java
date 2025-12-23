package com.ihm.backend.repository;

import com.ihm.backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
