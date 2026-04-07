package com.ihm.backend.repository;

import com.ihm.backend.entity.CourseComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseCommentRepository extends JpaRepository<CourseComment, Long> {

    List<CourseComment> findByCourse_IdOrderByCreatedAtDesc(Integer courseId);

    List<CourseComment> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    long countByCourse_Id(Integer courseId);
}
