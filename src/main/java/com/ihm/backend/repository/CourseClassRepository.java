package com.ihm.backend.repository;

import com.ihm.backend.entity.CourseClass;
import com.ihm.backend.enums.ClassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseClassRepository extends JpaRepository<CourseClass, Long> {

    /**
     * Classes d'un enseignant donné
     */
    List<CourseClass> findByTeacher_Id(UUID teacherId);

    /**
     * Classes par statut (ex: toutes les OPEN)
     */
    List<CourseClass> findByStatus(ClassStatus status);

    /**
     * Classes par thème et statut
     */
    List<CourseClass> findByThemeAndStatus(String theme, ClassStatus status);

    /**
     * Vérifie si un enseignant possède déjà une classe avec ce nom
     */
    boolean existsByNameAndTeacher_Id(String name, UUID teacherId);

    /**
     * Nombre de classes par statut
     */
    long countByStatus(ClassStatus status);

    /**
     * Nombre de classes d'un enseignant
     */
    long countByTeacher_Id(UUID teacherId);

    /**
     * Classes OPEN avec une recherche textuelle sur le nom ou la description
     */
    @Query("SELECT c FROM CourseClass c WHERE c.status = 'OPEN' " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.theme) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<CourseClass> searchOpenClasses(String query);
}
