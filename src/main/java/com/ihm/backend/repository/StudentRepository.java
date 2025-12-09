// StudentRepository.java
package com.ihm.backend.repository;

import com.ihm.backend.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
    // Tu peux ajouter des méthodes spécifiques si besoin
}