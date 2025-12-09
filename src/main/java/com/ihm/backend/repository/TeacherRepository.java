// TeacherRepository.java
package com.ihm.backend.repository;

import com.ihm.backend.domain.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, UUID> {
    // Méthodes spécifiques aux profs si besoin
}