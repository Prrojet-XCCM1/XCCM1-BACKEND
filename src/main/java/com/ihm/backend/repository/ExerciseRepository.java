package com.ihm.backend.repository;

import com.ihm.backend.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Integer> {
    
    List<Exercise> findByCourse_Id(Integer courseId);
    
    long countByCourse_Id(Integer courseId);
    
    List<Exercise> findByCourse_Author_Id(java.util.UUID authorId);
}
