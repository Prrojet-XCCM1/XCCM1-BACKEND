package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.ExerciseCreateRequest;
import com.ihm.backend.dto.request.ExerciseUpdateRequest;
import com.ihm.backend.dto.request.GradeSubmissionRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.ExerciseResponse;
import com.ihm.backend.dto.response.StudentExerciseResponse;
import com.ihm.backend.dto.response.TeacherCourseStatsResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.GlobalExceptionHandler;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.security.CustomAccessDeniedHandler;
import com.ihm.backend.security.oauth2.CustomOAuth2UserService;
import com.ihm.backend.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.ihm.backend.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.ihm.backend.security.JwtAuthenticationEntryPoint;
import com.ihm.backend.security.JwtAuthenticationFilter;
import com.ihm.backend.service.ExerciseService;
import com.ihm.backend.service.TeacherStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests unitaires pour TeacherController.
 * Utilise @WebMvcTest pour un test de tranche (slice test) focalisé sur la couche Web.
 */
@WebMvcTest(TeacherController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false) // Désactive les filtres de sécurité pour simplifier le test du contrôleur
class TeacherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeacherStatsService teacherStatsService;

    @MockitoBean
    private ExerciseService exerciseService;

    // Mocks requis pour charger le contexte d'application minimal (SecurityConfig)
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    @Autowired
    private ObjectMapper objectMapper;

    private User teacherUser;
    private final UUID teacherId = UUID.fromString("606940f3-450f-4347-870d-9653e4552154");

    @BeforeEach
    void setUp() {
        teacherUser = User.builder()
                .id(teacherId)
                .email("teacher@test.com")
                .role(UserRole.TEACHER)
                .build();

        // Simulation du principal d'authentification pour @AuthenticationPrincipal
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(teacherUser, null, List.of())
        );
    }

    @Nested
    @DisplayName("Statistiques de l'Enseignant")
    class Statistics {
        @Test
        @DisplayName("GET /api/v1/teacher/courses/stats - Récupération réussie")
        void getAllCoursesStatistics_success() throws Exception {
            TeacherCourseStatsResponse stats = TeacherCourseStatsResponse.builder().courseTitle("Java").build();
            when(teacherStatsService.getAllCoursesStatistics(teacherId))
                    .thenReturn(ApiResponse.success("Succès", List.of(stats)));

            mockMvc.perform(get("/api/v1/teacher/courses/stats"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].courseTitle").value("Java"));
        }

        @Test
        @DisplayName("GET /api/v1/teacher/courses/stats - Erreur Interne (500)")
        void getAllCoursesStatistics_internalError() throws Exception {
            when(teacherStatsService.getAllCoursesStatistics(teacherId))
                    .thenThrow(new RuntimeException("Erreur de base de données"));

            mockMvc.perform(get("/api/v1/teacher/courses/stats"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Une erreur interne est survenue"));
        }
    }

    @Nested
    @DisplayName("Gestion des Exercices")
    class ExerciseManagement {
        @Test
        @DisplayName("POST /api/v1/teacher/courses/{id}/exercises - Création réussie")
        void createExercise_success() throws Exception {
            ExerciseCreateRequest req = new ExerciseCreateRequest();
            req.setTitle("Nouvel Exercice");
            com.ihm.backend.dto.response.ExerciseResponse res = com.ihm.backend.dto.response.ExerciseResponse.builder()
                    .id(1).title("Nouvel Exercice").build();

            when(exerciseService.createExercise(eq(10), eq(teacherId), any()))
                    .thenReturn(ApiResponse.created("Succès", res));

            mockMvc.perform(post("/api/v1/teacher/courses/10/exercises")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.title").value("Nouvel Exercice"));
        }

        @Test
        @DisplayName("POST /api/v1/teacher/courses/{id}/exercises - Ressource non trouvée (404)")
        void createExercise_notFound() throws Exception {
            ExerciseCreateRequest req = new ExerciseCreateRequest();
            req.setTitle("Test");

            when(exerciseService.createExercise(eq(999), eq(teacherId), any()))
                    .thenThrow(new ResourceNotFoundException("Cours non trouvé"));

            mockMvc.perform(post("/api/v1/teacher/courses/999/exercises")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cours non trouvé"));
        }

        @Test
        @DisplayName("PUT /api/v1/teacher/submissions/{id}/grade - Notation réussie")
        void gradeSubmission_success() throws Exception {
            GradeSubmissionRequest req = new GradeSubmissionRequest();
            req.setScore(15.5);
            StudentExerciseResponse res = StudentExerciseResponse.builder().id(1L).score(15.5).build();

            when(exerciseService.gradeSubmission(eq(100L), eq(teacherId), any()))
                    .thenReturn(ApiResponse.success("Success", res));

            mockMvc.perform(put("/api/v1/teacher/submissions/100/grade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.score").value(15.5));
        }

        @Test
        @DisplayName("PUT /api/v1/teacher/submissions/{id}/grade - Mauvaise requête (400)")
        void gradeSubmission_badRequest() throws Exception {
            GradeSubmissionRequest req = new GradeSubmissionRequest();
            req.setScore(100.0);

            when(exerciseService.gradeSubmission(eq(100L), eq(teacherId), any()))
                    .thenThrow(new IllegalArgumentException("Note invalide"));

            mockMvc.perform(put("/api/v1/teacher/submissions/100/grade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Note invalide"));
        }
    }
}
