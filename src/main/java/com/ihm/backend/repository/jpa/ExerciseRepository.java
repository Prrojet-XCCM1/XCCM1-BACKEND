package com.ihm.backend.repository.jpa;

import com.ihm.backend.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Integer> {
    
    List<Exercise> findByCourse_Id(Integer courseId);
    
    long countByCourse_Id(Integer courseId);
    
    List<Exercise> findByCourse_Author_Id(java.util.UUID authorId);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Exercise e WHERE " +
           "LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Exercise> searchExercises(@org.springframework.data.repository.query.Param("query") String query);
}
