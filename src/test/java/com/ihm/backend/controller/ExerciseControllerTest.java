package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.StudentExerciseSubmissionRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.ExerciseResponse;
import com.ihm.backend.dto.response.StudentExerciseResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.GlobalExceptionHandler;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.security.CustomAccessDeniedHandler;
import com.ihm.backend.security.JwtAuthenticationEntryPoint;
import com.ihm.backend.security.JwtAuthenticationFilter;
import com.ihm.backend.service.ExerciseService;
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
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExerciseController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class ExerciseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExerciseService exerciseService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    private ObjectMapper objectMapper;

    private User studentUser;
    private final UUID studentId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(studentId)
                .email("student@test.com")
                .role(UserRole.STUDENT)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(studentUser, null, List.of())
        );
    }

    @Nested
    @DisplayName("Consultation des Exercices")
    class ExerciseViewing {
        @Test
        @DisplayName("GET /api/v1/exercises/course/{courseId} - Succès")
        void getExercisesForCourse_success() throws Exception {
            ExerciseResponse res = ExerciseResponse.builder().id(1).title("Exo 1").build();
            when(exerciseService.getExercisesForCourse(eq(10), eq(studentId)))
                    .thenReturn(ApiResponse.success("Succès", List.of(res)));

            mockMvc.perform(get("/api/v1/exercises/course/10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].title").value("Exo 1"));
        }

        @Test
        @DisplayName("GET /api/v1/exercises/{exerciseId} - Exercice non trouvé (404)")
        void getExerciseDetails_notFound() throws Exception {
            when(exerciseService.getExerciseDetails(eq(999), eq(studentId)))
                    .thenThrow(new ResourceNotFoundException("Exercice non trouvé"));

            mockMvc.perform(get("/api/v1/exercises/999"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("Soumission d'Exercices")
    class Submissions {
        @Test
        @DisplayName("POST /api/v1/exercises/{exerciseId}/submit - Succès")
        void submitExercise_success() throws Exception {
            StudentExerciseSubmissionRequest req = new StudentExerciseSubmissionRequest();
            req.setContent(Map.of("answer", "Test Answer"));
            StudentExerciseResponse res = StudentExerciseResponse.builder().id(1L).build();

            when(exerciseService.submitExercise(eq(1), eq(studentId), any()))
                    .thenReturn(ApiResponse.success("Succès", res));

            mockMvc.perform(post("/api/v1/exercises/1/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @DisplayName("POST /api/v1/exercises/{exerciseId}/submit - Erreur Interne (500)")
        void submitExercise_internalError() throws Exception {
            StudentExerciseSubmissionRequest req = new StudentExerciseSubmissionRequest();
            req.setContent(Map.of("answer", "Test"));

            when(exerciseService.submitExercise(eq(1), eq(studentId), any()))
                    .thenThrow(new RuntimeException("Crash"));

            mockMvc.perform(post("/api/v1/exercises/1/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("Mes Soumissions")
    class MySubmissions {
        @Test
        @DisplayName("GET /api/v1/exercises/my-submissions - Liste des soumissions")
        void getMySubmissions_success() throws Exception {
            StudentExerciseResponse res = StudentExerciseResponse.builder().id(1L).score(10.0).build();
            when(exerciseService.getMySubmissions(studentId))
                    .thenReturn(ApiResponse.success("Succès", List.of(res)));

            mockMvc.perform(get("/api/v1/exercises/my-submissions"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }
}
