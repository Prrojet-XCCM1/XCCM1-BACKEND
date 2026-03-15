package com.ihm.backend.repository;

import com.ihm.backend.entity.CourseLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CourseLikeRepository extends JpaRepository<CourseLike, Long> {

    boolean existsByCourse_IdAndUser_Id(Integer courseId, UUID userId);

    void deleteByCourse_IdAndUser_Id(Integer courseId, UUID userId);

    long countByCourse_Id(Integer courseId);
}
