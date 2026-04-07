package com.ihm.backend.repository;

import com.ihm.backend.entity.StudentExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentExerciseRepository extends JpaRepository<StudentExercise, Long> {
    
    List<StudentExercise> findByExercise_Id(Integer exerciseId);
    
    List<StudentExercise> findByStudent_Id(UUID studentId);
    
    List<StudentExercise> findByExercise_Course_Id(Integer courseId);
    
    long countByExercise_Id(Integer exerciseId);
    
    @Query("SELECT AVG(se.score) FROM StudentExercise se WHERE se.exercise.id = :exerciseId AND se.score IS NOT NULL")
    Double calculateAverageScore(@Param("exerciseId") Integer exerciseId);
    
    @Query("SELECT MIN(se.score) FROM StudentExercise se WHERE se.exercise.id = :exerciseId AND se.score IS NOT NULL")
    Double findMinScore(@Param("exerciseId") Integer exerciseId);
    
    @Query("SELECT MAX(se.score) FROM StudentExercise se WHERE se.exercise.id = :exerciseId AND se.score IS NOT NULL")
    Double findMaxScore(@Param("exerciseId") Integer exerciseId);
    
    @Query("SELECT COUNT(se) FROM StudentExercise se WHERE se.exercise.course.id = :courseId AND se.score >= :minScore AND se.score < :maxScore")
    long countByScoreRange(@Param("courseId") Integer courseId, @Param("minScore") Double minScore, @Param("maxScore") Double maxScore);
}
