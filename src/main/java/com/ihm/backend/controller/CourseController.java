package com.ihm.backend.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ihm.backend.enums.CourseStatus;
import com.ihm.backend.service.CourseService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ihm.backend.dto.request.*;
import com.ihm.backend.dto.response.EnrichedCourseResponse;
import com.ihm.backend.entity.User;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
public class CourseController {
    @Autowired
    private CourseService courseService;
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/{authorId}")
    public ResponseEntity<?> createCourse(@RequestBody CourseCreateRequest request,
                                           @PathVariable UUID authorId,
                                           Authentication authentication) {
        try {
            // Vérifier que l'enseignant crée un cours pour lui-même
            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getId().equals(authorId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Vous ne pouvez créer un cours que pour vous-même");
            }
            
            return ResponseEntity.status(HttpStatus.CREATED)
                     .body(courseService.createCourse(request, authorId));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }



    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{authorId}")
    public ResponseEntity<?> getAuthorCourses(@PathVariable UUID authorId,
                                                Authentication authentication) {
        try {
            // Vérifier que l'enseignant accède à ses propres cours
            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getId().equals(authorId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Vous ne pouvez consulter que vos propres cours");
            }
            
            return ResponseEntity.status(HttpStatus.OK)
                     .body(courseService.getAllCoursesForTeacher(authorId));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/{courseId}/coverImage/upload")
    public ResponseEntity<?> uploadImage(@PathVariable Integer courseId,
                                          @RequestParam MultipartFile image,
                                          Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            courseService.validateOwnership(courseId, currentUser.getId());
            
            return ResponseEntity.status(HttpStatus.OK)
                     .body(courseService.uploadCoverImage(courseId, image));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{courseId}/setStatus/{status}")
    public ResponseEntity<?> changeCourseStatus(@PathVariable Integer courseId,
                                                 @PathVariable CourseStatus status,
                                                 Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            courseService.validateOwnership(courseId, currentUser.getId());
            
            return ResponseEntity.status(HttpStatus.OK)
                     .body(courseService.changeCourseStatus(status, courseId));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/{courseId}/status")
    public ResponseEntity<?> updateCourseStatus(@PathVariable Integer courseId,
                                                 @RequestParam CourseStatus status,
                                                 Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            courseService.validateOwnership(courseId, currentUser.getId());
            
            return ResponseEntity.status(HttpStatus.OK)
                     .body(courseService.changeCourseStatus(status, courseId));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
     @GetMapping("/{authorId}/status/{status}")
    public ResponseEntity<?> getCoureByStatusForAuthor(@PathVariable Integer authorId,@PathVariable CourseStatus status) {
        try {
          return ResponseEntity.status(HttpStatus.OK)
                     .body(courseService.getCoursesByStatusForAuthor(authorId,status));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


         @GetMapping
    public ResponseEntity<?> getAllCourses() {
        try {
          return ResponseEntity.status(HttpStatus.OK)
                     .body(courseService.getAllCourses());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{courseId}")
    public ResponseEntity<?> updateCourse(@PathVariable Integer courseId,
                                           @RequestBody CourseUpdateRequest request,
                                           Authentication authentication) {
       try {
           User currentUser = (User) authentication.getPrincipal();
           courseService.validateOwnership(courseId, currentUser.getId());
           
           return ResponseEntity.status(HttpStatus.OK)
                      .body(courseService.updateCourse(courseId, request));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{courseId}")
    public ResponseEntity<?> deleteCourse(@PathVariable Integer courseId,
                                           Authentication authentication) {
       try {
           User currentUser = (User) authentication.getPrincipal();
           courseService.validateOwnership(courseId, currentUser.getId());
           
           courseService.deleteCourse(courseId);
           return ResponseEntity.status(HttpStatus.OK)
                      .body("Course deleted");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Récupérer tous les cours enrichis avec les enrôlements de l'utilisateur courant
     * Accessible à tous les utilisateurs authentifiés
     */
    @GetMapping("/enriched")
    public ResponseEntity<?> getEnrichedCourses(Authentication authentication) {
        try {
            UUID userId = null;
            if (authentication != null) {
                User currentUser = (User) authentication.getPrincipal();
                userId = currentUser.getId();
            }
            
            List<EnrichedCourseResponse> enrichedCourses = courseService.getEnrichedCourses(userId);
            return ResponseEntity.ok(enrichedCourses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Récupérer un cours enrichi spécifique
     */
    @GetMapping("/enriched/{courseId}")
    public ResponseEntity<?> getEnrichedCourse(@PathVariable Integer courseId,
                                                Authentication authentication) {
        try {
            UUID userId = null;
            if (authentication != null) {
                User currentUser = (User) authentication.getPrincipal();
                userId = currentUser.getId();
            }
            
            EnrichedCourseResponse enrichedCourse = courseService.getEnrichedCourse(courseId, userId);
            return ResponseEntity.ok(enrichedCourse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
