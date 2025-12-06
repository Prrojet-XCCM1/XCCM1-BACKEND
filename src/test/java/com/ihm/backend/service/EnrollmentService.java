package com.ihm.backend.service;

import com.ihm.backend.dto.EnrollmentDTO;
import com.ihm.backend.entity.Cours;
import com.ihm.backend.entity.Enrollment;
import com.ihm.backend.entity.Etudiant;
import com.ihm.backend.mapper.EnrollmentMapper;
import com.ihm.backend.repository.CoursRepository;
import com.ihm.backend.repository.EnrollmentRepository;
import com.ihm.backend.repository.EtudiantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private EtudiantRepository etudiantRepository;

    @Autowired
    private CoursRepository coursRepository;

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public EnrollmentDTO enroll(Long etudiantId, Long coursId) {
        if (enrollmentRepository.existsByEtudiantIdAndCoursId(etudiantId, coursId)) {
            throw new RuntimeException("Déjà inscrit à ce cours");
        }

        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));

        Cours cours = coursRepository.findById(coursId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        Enrollment enrollment = new Enrollment();
        enrollment.setEtudiant(etudiant);
        enrollment.setCours(cours);
        enrollment.setStatus(Enrollment.EnrollmentStatus.PENDING);  // Commence en attente

        Enrollment saved = enrollmentRepository.save(enrollment);

        kafkaTemplate.send("enrollment-created", saved);

        return enrollmentMapper.toDTO(saved);
    }

    @Transactional
    public EnrollmentDTO approveEnrollment(Long enrollmentId) {  // La méthode manquante ici
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));

        if (enrollment.getStatus() != Enrollment.EnrollmentStatus.PENDING) {
            throw new RuntimeException("Inscription déjà approuvée ou complétée");
        }

        enrollment.setStatus(Enrollment.EnrollmentStatus.ENROLLED);

        Enrollment updated = enrollmentRepository.save(enrollment);

        kafkaTemplate.send("enrollment-approved", updated);

        return enrollmentMapper.toDTO(updated);
    }

    @Transactional
    public EnrollmentDTO validateCompletion(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));

        enrollment.setStatus(Enrollment.EnrollmentStatus.COMPLETED);
        enrollment.setProgress(100.0);

        Enrollment updated = enrollmentRepository.save(enrollment);

        kafkaTemplate.send("course-completed", updated);

        return enrollmentMapper.toDTO(updated);
    }

    public List<EnrollmentDTO> getEnrollmentsForEtudiant(Long etudiantId) {
        return enrollmentRepository.findByEtudiantId(etudiantId).stream()
                .map(enrollmentMapper::toDTO)
                .collect(Collectors.toList());
    }
}