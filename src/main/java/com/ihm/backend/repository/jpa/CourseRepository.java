package com.ihm.backend.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ihm.backend.entity.*;
import java.util.List;
import com.ihm.backend.enums.CourseStatus;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.courseClass WHERE c.author = :author")
    List<Course> findByAuthor(@Param("author") User author);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.courseClass WHERE c.status = :status")
    List<Course> findByStatus(@Param("status") CourseStatus status);

    List<Course> findByStatusAndAuthor(CourseStatus status, User author);

    long countByStatus(CourseStatus status);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.courseClass WHERE c.author.id = :authorId")
    List<Course> findByAuthor_Id(@Param("authorId") UUID authorId);

    @Query("SELECT c FROM Course c WHERE c.status = 'PUBLISHED' " +
           "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.category) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Course> searchPublishedCourses(@Param("query") String query);
}
