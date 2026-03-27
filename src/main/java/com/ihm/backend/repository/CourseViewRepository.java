package com.ihm.backend.repository;

import com.ihm.backend.entity.CourseView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CourseViewRepository extends JpaRepository<CourseView, Long> {

    boolean existsByCourse_IdAndUser_Id(Integer courseId, UUID userId);

    long countByCourse_Id(Integer courseId);
}
