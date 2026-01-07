package com.ihm.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ihm.backend.entity.*;
import java.util.List;
import com.ihm.backend.enums.CourseStatus;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {
    List<Course> findByAuthor(User author);

    List<Course> findByStatus(CourseStatus status);

    List<Course> findByStatusAndAuthor(CourseStatus status, User author);

    long countByStatus(CourseStatus status);
}
