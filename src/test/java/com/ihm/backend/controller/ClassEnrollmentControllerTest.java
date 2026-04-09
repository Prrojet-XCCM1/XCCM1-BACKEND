package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.ClassEnrollmentDTO;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.EnrollmentStatus;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.GlobalExceptionHandler;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.security.CustomAccessDeniedHandler;
import com.ihm.backend.security.JwtAuthenticationEntryPoint;
import com.ihm.backend.security.JwtAuthenticationFilter;
import com.ihm.backend.service.ClassEnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(ClassEnrollmentController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class ClassEnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClassEnrollmentService enrollmentService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private User studentUser;
    private User teacherUser;
    private final UUID studentId = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private final UUID teacherId = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private ClassEnrollmentDTO sampleEnrollment;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(studentId)
                .email("student@test.com")
                .role(UserRole.STUDENT)
                .build();

        teacherUser = User.builder()
                .id(teacherId)
                .email("teacher@test.com")
                .role(UserRole.TEACHER)
                .build();

        sampleEnrollment = ClassEnrollmentDTO.builder()
                .id(10L)
                .classId(1L)
                .status(EnrollmentStatus.PENDING)
                .studentId(studentId.toString())
                .build();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }

    @Nested
    @DisplayName("Opérations Étudiant")
    class StudentOperations {
        @Test
        @DisplayName("POST /api/class-enrollments/{id} - Succès")
        void enrollInClass_success() throws Exception {
            authenticateAs(studentUser);
            when(enrollmentService.enrollInClass(eq(1L), eq(studentId))).thenReturn(sampleEnrollment);

            mockMvc.perform(post("/api/class-enrollments/1")
                            .with(user(studentUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("POST /api/class-enrollments/{id} - Classe complète (400)")
        void enrollInClass_full() throws Exception {
            authenticateAs(studentUser);
            when(enrollmentService.enrollInClass(eq(1L), eq(studentId)))
                    .thenThrow(new IllegalStateException("La classe est complète"));

            mockMvc.perform(post("/api/class-enrollments/1")
                            .with(user(studentUser)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("DELETE /api/class-enrollments/{id} - Désinscription réussie")
        void unenrollFromClass_success() throws Exception {
            authenticateAs(studentUser);
            doNothing().when(enrollmentService).unenrollFromClass(eq(10L), eq(studentId));

            mockMvc.perform(delete("/api/class-enrollments/10")
                            .with(user(studentUser)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Opérations Enseignant")
    class TeacherOperations {
        @Test
        @DisplayName("GET /api/class-enrollments/pending - Succès")
        void getPendingEnrollments_success() throws Exception {
            authenticateAs(teacherUser);
            when(enrollmentService.getPendingForTeacher(eq(teacherId))).thenReturn(List.of(sampleEnrollment));

            mockMvc.perform(get("/api/class-enrollments/pending")
                            .with(user(teacherUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("PUT /api/class-enrollments/{id}/validate - Approbation réussie")
        void validateEnrollment_success() throws Exception {
            authenticateAs(teacherUser);
            ClassEnrollmentDTO approved = ClassEnrollmentDTO.builder().id(10L).status(EnrollmentStatus.APPROVED).build();
            when(enrollmentService.validateEnrollment(eq(10L), eq(EnrollmentStatus.APPROVED), eq(teacherId)))
                    .thenReturn(approved);

            mockMvc.perform(put("/api/class-enrollments/10/validate")
                            .with(user(teacherUser))
                            .param("status", "APPROVED"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andExpect(jsonPath("$.message").value("Étudiant validé avec succès"));
        }

        @Test
        @DisplayName("PUT /api/class-enrollments/{id}/validate - Erreur 403 (Non-propriétaire)")
        void validateEnrollment_forbidden() throws Exception {
            authenticateAs(teacherUser);
            when(enrollmentService.validateEnrollment(eq(10L), eq(EnrollmentStatus.APPROVED), eq(teacherId)))
                    .thenThrow(new org.springframework.security.access.AccessDeniedException("Accès refusé"));

            mockMvc.perform(put("/api/class-enrollments/10/validate")
                            .with(user(teacherUser))
                            .param("status", "APPROVED"))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
