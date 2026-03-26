package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.ExerciseCreateRequest;
import com.ihm.backend.dto.request.GradeSubmissionRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.ExerciseResponse;
import com.ihm.backend.dto.response.StudentExerciseResponse;
import com.ihm.backend.dto.response.TeacherCourseStatsResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.service.ExerciseService;
import com.ihm.backend.service.TeacherStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeacherControllerTest {

    @Mock
    private TeacherStatsService teacherStatsService;

    @Mock
    private ExerciseService exerciseService;

    @InjectMocks
    private TeacherController teacherController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private User teacherUser;
    private UUID teacherId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(teacherController).build();

        teacherUser = User.builder()
                .id(teacherId)
                .email("teacher@test.com")
                .role(UserRole.TEACHER)
                .build();
    }

    private RequestPostProcessor asTeacher() {
        return (MockHttpServletRequest request) -> {
            request.setUserPrincipal(
                    new UsernamePasswordAuthenticationToken(teacherUser, null,
                            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
            return request;
        };
    }

    @Nested
    @DisplayName("Statistiques (TEACHER)")
    class Statistics {
        @Test
        @DisplayName("GET /api/v1/teacher/courses/stats - Toutes les stats")
        void getAllCoursesStatistics_success() throws Exception {
            TeacherCourseStatsResponse stats = TeacherCourseStatsResponse.builder().courseTitle("Java").build();
            when(teacherStatsService.getAllCoursesStatistics(teacherId)).thenReturn(ApiResponse.success("Success", List.of(stats)));

            mockMvc.perform(get("/api/v1/teacher/courses/stats").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].courseTitle").value("Java"));
        }
    }

    @Nested
    @DisplayName("Gestion des Exercices (TEACHER)")
    class ExerciseManagement {
        @Test
        @DisplayName("POST /api/v1/teacher/courses/{courseId}/exercises - Créer un exo")
        void createExercise_success() throws Exception {
            ExerciseCreateRequest req = new ExerciseCreateRequest();
            req.setTitle("New Exercise");
            ExerciseResponse res = ExerciseResponse.builder().id(1).title("New Exercise").build();

            when(exerciseService.createExercise(eq(10), eq(teacherId), any())).thenReturn(ApiResponse.success("Success", res));

            mockMvc.perform(post("/api/v1/teacher/courses/10/exercises")
                            .with(asTeacher())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.title").value("New Exercise"));
        }

        @Test
        @DisplayName("PUT /api/v1/teacher/submissions/{id}/grade - Noter soumission")
        void gradeSubmission_success() throws Exception {
            GradeSubmissionRequest req = new GradeSubmissionRequest();
            req.setScore(15.5);
            StudentExerciseResponse res = StudentExerciseResponse.builder().id(1L).score(15.5).build();

            when(exerciseService.gradeSubmission(eq(100L), eq(teacherId), any())).thenReturn(ApiResponse.success("Success", res));

            mockMvc.perform(put("/api/v1/teacher/submissions/100/grade")
                            .with(asTeacher())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.grade").value(15.5));
        }
    }
}
