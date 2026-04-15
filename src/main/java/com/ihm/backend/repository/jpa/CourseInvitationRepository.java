package com.ihm.backend.repository.jpa;

import com.ihm.backend.entity.CourseInvitation;
import com.ihm.backend.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseInvitationRepository extends JpaRepository<CourseInvitation, Long> {
    Optional<CourseInvitation> findByToken(String token);
    List<CourseInvitation> findByEmailAndStatus(String email, InvitationStatus status);
    List<CourseInvitation> findByCourse_IdAndStatus(Integer courseId, InvitationStatus status);
    void deleteByExpiryDateBeforeAndStatus(LocalDateTime now, InvitationStatus status);
}
