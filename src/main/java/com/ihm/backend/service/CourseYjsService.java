package com.ihm.backend.service;

import com.ihm.backend.repository.jpa.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseYjsService {

    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public byte[] getYjsState(Integer courseId) {
        return courseRepository.findById(courseId)
                .map(course -> course.getYjsState())
                .orElse(null);
    }

    @Transactional
    public void saveYjsState(Integer courseId, byte[] state) {
        courseRepository.findById(courseId).ifPresentOrElse(
                course -> {
                    course.setYjsState(state);
                    courseRepository.save(course);
                    log.debug("État Y.js sauvegardé pour le cours {}", courseId);
                },
                () -> log.warn("Cours {} introuvable pour la sauvegarde Y.js", courseId)
        );
    }
}
