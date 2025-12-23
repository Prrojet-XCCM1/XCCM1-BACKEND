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
     */
    Optional<Enrollment> findByCourseIdAndUserId(Integer courseId, UUID userId);
    
    /**
     * Trouve tous les enrôlements d'un utilisateur
     */
    List<Enrollment> findByUserId(UUID userId);
    
    /**
     * Trouve tous les enrôlements pour un cours donné
     */
    List<Enrollment> findByCourseId(Integer courseId);
    
    /**
     * Vérifie si un utilisateur est déjà enrôlé à un cours
     */
    boolean existsByCourseIdAndUserId(Integer courseId, UUID userId);
}
