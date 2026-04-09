package com.ihm.backend.controller;

import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.entity.Course;
import com.ihm.backend.entity.CourseClass;
import com.ihm.backend.entity.Exercise;
import com.ihm.backend.entity.User;
import com.ihm.backend.service.CourseClassService;
import com.ihm.backend.service.CourseService;
import com.ihm.backend.service.ExerciseService;
import com.ihm.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@CrossOrigin
public class SearchController {

    private final UserService userService;
    private final CourseService courseService;
    private final CourseClassService classService;
    private final ExerciseService exerciseService;

    @GetMapping("/users")
    public ResponseEntity<Page<User>> searchUsers(
            @RequestParam String query,
            Pageable pageable) {
        return ResponseEntity.ok(userService.searchUsers(query, pageable));
    }

    @GetMapping("/courses")
    public ResponseEntity<List<Course>> searchCourses(@RequestParam String query) {
        return ResponseEntity.ok(courseService.searchCourses(query));
    }

    @GetMapping("/classes")
    public ResponseEntity<List<CourseClass>> searchClasses(@RequestParam String query) {
        return ResponseEntity.ok(classService.searchClasses(query));
    }

    @GetMapping("/exercises")
    public ResponseEntity<List<Exercise>> searchExercises(@RequestParam String query) {
        return ResponseEntity.ok(exerciseService.searchExercises(query));
    }
}
