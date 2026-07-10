package com.ihm.backend.repository.jpa;

import com.ihm.backend.entity.CourseGenerationJob;
import com.ihm.backend.enums.CourseGenerationJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseGenerationJobRepository extends JpaRepository<CourseGenerationJob, UUID> {

    Optional<CourseGenerationJob> findByIdAndUserId(UUID id, UUID userId);

    List<CourseGenerationJob> findByUserIdAndStatusInOrderByCreatedAtDesc(
            UUID userId,
            Collection<CourseGenerationJobStatus> statuses
    );
}
