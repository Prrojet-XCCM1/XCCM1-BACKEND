package com.ihm.backend.controller;

import com.ihm.backend.dto.EnrollmentDTO;
import com.ihm.backend.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    @PostMapping("/enroll/{coursId}")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<EnrollmentDTO> enroll(@PathVariable Long coursId, @RequestParam Long etudiantId) {  // Assume etudiantId from auth, but for simplicity
        EnrollmentDTO dto = enrollmentService.enroll(etudiantId, coursId);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/validate/{enrollmentId}")
    @PreAuthorize("hasRole('ENSEIGNANT') or hasRole('ADMIN')")  // Validation par teacher ou admin
    public ResponseEntity<EnrollmentDTO> validateCompletion(@PathVariable Long enrollmentId) {
        EnrollmentDTO dto = enrollmentService.validateCompletion(enrollmentId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/etudiant/{etudiantId}")
    @PreAuthorize("hasRole('ETUDIANT') or hasRole('ADMIN')")
    public ResponseEntity<List<EnrollmentDTO>> getEnrollments(@PathVariable Long etudiantId) {
        List<EnrollmentDTO> enrollments = enrollmentService.getEnrollmentsForEtudiant(etudiantId);
        return ResponseEntity.ok(enrollments);
    }
    @PutMapping("/approve/{enrollmentId}")
    @PreAuthorize("hasRole('ENSEIGNANT') or hasRole('ADMIN')")  // Approbation par enseignant ou admin
    public ResponseEntity<EnrollmentDTO> approveEnrollment(@PathVariable Long enrollmentId) {
        EnrollmentDTO dto = enrollmentService.approveEnrollment(enrollmentId);
        return ResponseEntity.ok(dto);
    }
}