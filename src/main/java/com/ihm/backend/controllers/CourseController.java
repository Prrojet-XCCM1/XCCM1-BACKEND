package com.ihm.backend.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ihm.backend.DTO.responses.CourseResponse;
import com.ihm.backend.enums.CourseStatus;
import com.ihm.backend.services.CourseService;

import jakarta.mail.Multipart;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ihm.backend.DTO.requests.*;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/courses")
public class CourseController {
    @Autowired
    private CourseService courseService;
    @PostMapping("/{authorId}")
    public ResponseEntity<?> createCourse(@RequestBody CourseCreateRequestDTO request,@PathVariable Integer authorId) {
        try {
          return ResponseEntity.status(HttpStatus.CREATED)
                     .body(courseService.createCourse(request, authorId));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }



      @GetMapping("/{authorId}")
    public ResponseEntity<?> getAuthorCourses(@PathVariable Integer authorId) {
        try {
          return ResponseEntity.status(HttpStatus.OK)
                     .body(courseService.getAllCoursesForTeacher(authorId));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{courseId}/coverImage/upload")
    public ResponseEntity<?> uploadImage(@PathVariable Integer courseId,@RequestParam MultipartFile image) {
        try {
            return ResponseEntity.status(HttpStatus.OK)
                     .body(courseService.uploadCoverImage(courseId, image));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        
        
    }
    

      @GetMapping("/{courseId}/setStatus/{status}")
    public ResponseEntity<?> changeCourseStatus(@PathVariable Integer courseId,@PathVariable CourseStatus status) {
        try {
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
    
    @PutMapping("/{courseId}")
    public ResponseEntity<?> updateCourse(@PathVariable Integer courseId, @RequestBody CourseUpdateRequestDTO request) {
       try {
          return ResponseEntity.status(HttpStatus.OK)
                     .body(courseService.updateCourse(courseId, request));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
     @DeleteMapping("/{courseId}")
    public ResponseEntity<?> deleteCourse(@PathVariable Integer courseId) {
       try {
        courseService.deleteCourse(courseId);
          return ResponseEntity.status(HttpStatus.OK)
                     .body("Course deleted");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    


    
}
