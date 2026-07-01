package com.ihm.backend.controller;

import com.ihm.backend.entity.Course;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.EnrollmentStatus;
import com.ihm.backend.repository.jpa.CourseRepository;
import com.ihm.backend.repository.jpa.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Vérifie qu'un utilisateur JWT peut accéder au document (cours) donné.
 * Appelé par Hocuspocus lors de la connexion WebSocket Y.js.
 */
@RestController
@RequestMapping("/api/collaboration")
@RequiredArgsConstructor
public class CollaborationAccessController {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @GetMapping("/access/{courseId}")
    public ResponseEntity<Void> verifyAccess(
            @PathVariable Integer courseId,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(403).build();
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null || !hasAccess(course, user)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok().build();
    }

    private boolean hasAccess(Course course, User user) {
        if (course.getAuthor() != null && course.getAuthor().getEmail().equals(user.getEmail())) {
            return true;
        }
        if (course.getEditors() != null &&
                course.getEditors().stream().anyMatch(u -> u.getEmail().equals(user.getEmail()))) {
            return true;
        }
        return enrollmentRepository.findByCourse_IdAndUser_Id(course.getId(), user.getId())
                .map(e -> e.getStatus() == EnrollmentStatus.APPROVED || e.getStatus() == EnrollmentStatus.INVITED)
                .orElse(false);
    }
}
