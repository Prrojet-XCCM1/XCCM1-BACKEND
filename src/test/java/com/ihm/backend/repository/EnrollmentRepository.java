package com.ihm.backend.repository;

import com.ihm.backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByEtudiantId(Long etudiantId);
    List<Enrollment> findByCoursId(Long coursId);
    boolean existsByEtudiantIdAndCoursId(Long etudiantId, Long coursId);
}