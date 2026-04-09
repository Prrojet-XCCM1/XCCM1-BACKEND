package com.ihm.backend.controller;

import com.ihm.backend.dto.EnrollmentDTO;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.EnrollmentStatus;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.GlobalExceptionHandler;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.security.CustomAccessDeniedHandler;
import com.ihm.backend.security.JwtAuthenticationEntryPoint;
import com.ihm.backend.security.JwtAuthenticationFilter;
import com.ihm.backend.service.EnrollmentService;
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
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnrollmentController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private User studentUser;
    private User teacherUser;
    private EnrollmentDTO studentEnrollment;
    private EnrollmentDTO teacherEnrollment;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .email("student@test.com")
                .password("password")
                .role(UserRole.STUDENT)
                .firstName("John")
                .lastName("Doe")
                .active(true)
                .verified(true)
                .build();

        teacherUser = User.builder()
                .id(UUID.fromString("55555555-5555-5555-5555-555555555555"))
                .email("teacher@test.com")
                .password("password")
                .role(UserRole.TEACHER)
                .firstName("Jane")
                .lastName("Smith")
                .active(true)
                .verified(true)
                .build();

        studentEnrollment = EnrollmentDTO.builder()
                .id(1L)
                .courseId(10)
                .userId(studentUser.getId().toString())
                .enrolledAt(LocalDateTime.now())
                .progress(0.0)
                .completed(false)
                .status(EnrollmentStatus.PENDING)
                .build();

        teacherEnrollment = EnrollmentDTO.builder()
                .id(2L)
                .courseId(10)
                .userId(teacherUser.getId().toString())
                .enrolledAt(LocalDateTime.now())
                .progress(0.0)
                .completed(false)
                .status(EnrollmentStatus.APPROVED)
                .build();
    }

    /**
     * Crée un objet Authentication à partir d'un User.
     * Passe getAuthorities() pour que @PreAuthorize("hasRole(...)") fonctionne
     * même avec addFilters = false.
     */
    private Authentication auth(User user) {
        return new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
    }
    
    private void authenticateAs(User user) {
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .setAuthentication(auth(user));
    }

    // =========================================================================
    // ENRÔLEMENT
    // =========================================================================

    @Nested
    @DisplayName("Enrôlement à un cours")
    class EnrollInCourse {

        @Test
        @DisplayName("POST /api/enrollments/courses/{id} - Succès (Étudiant)")
        void student_canEnroll_success() throws Exception {
            when(enrollmentService.enrollUser(eq(10), eq(studentUser.getId())))
                    .thenReturn(studentEnrollment);

            authenticateAs(studentUser);
            mockMvc.perform(post("/api/enrollments/courses/10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.message").value("Enrôlement réussi"));
        }

        @Test
        @DisplayName("POST /api/enrollments/courses/{id} - Déjà enrôlé (400)")
        void alreadyEnrolled_error() throws Exception {
            when(enrollmentService.enrollUser(eq(10), eq(studentUser.getId())))
                    .thenThrow(new IllegalStateException("Vous êtes déjà enrôlé à ce cours"));

            authenticateAs(studentUser);
            mockMvc.perform(post("/api/enrollments/courses/10"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Vous êtes déjà enrôlé à ce cours"));
        }
    }

    // =========================================================================
    // MES ENRÔLEMENTS
    // =========================================================================

    @Nested
    @DisplayName("Mes Enrôlements")
    class MyEnrollments {

        @Test
        @DisplayName("GET /api/enrollments/my-courses - Succès")
        void getMyEnrollments_success() throws Exception {
            when(enrollmentService.getUserEnrollments(studentUser.getId()))
                    .thenReturn(List.of(studentEnrollment));

            authenticateAs(studentUser);
            mockMvc.perform(get("/api/enrollments/my-courses"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(1));
        }
    }

    // =========================================================================
    // STATUT D'ENRÔLEMENT
    // =========================================================================

    @Nested
    @DisplayName("Statut d'Enrôlement")
    class StatusCheck {

        @Test
        @DisplayName("GET /api/enrollments/courses/{id} - Trouvé")
        void getEnrollmentForCourse_found() throws Exception {
            when(enrollmentService.getEnrollmentForUser(eq(10), eq(studentUser.getId())))
                    .thenReturn(studentEnrollment);

            authenticateAs(studentUser);
            mockMvc.perform(get("/api/enrollments/courses/10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Enrôlement trouvé"));
        }

        @Test
        @DisplayName("GET /api/enrollments/courses/{id} - Non enrôlé (200 + message)")
        void getEnrollmentForCourse_notEnrolled() throws Exception {
            when(enrollmentService.getEnrollmentForUser(eq(99), eq(studentUser.getId())))
                    .thenReturn(null);

            authenticateAs(studentUser);
            mockMvc.perform(get("/api/enrollments/courses/99"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Non enrôlé"));
        }
    }

    // =========================================================================
    // VALIDATION (ENSEIGNANT)
    // =========================================================================

    @Nested
    @DisplayName("Validation (Enseignant)")
    class TeacherValidation {

        @Test
        @DisplayName("PUT /api/enrollments/{id}/validate - Approbation réussie")
        void validateEnrollment_approved() throws Exception {
            when(enrollmentService.validateEnrollment(
                    eq(1L), eq(EnrollmentStatus.APPROVED), eq(teacherUser.getId())))
                    .thenReturn(teacherEnrollment);

            authenticateAs(teacherUser);
            mockMvc.perform(put("/api/enrollments/1/validate")
                            .param("status", "APPROVED"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("APPROVED"));
        }

        @Test
        @DisplayName("PUT /api/enrollments/{id}/validate - Refus réussi")
        void validateEnrollment_rejected() throws Exception {
            EnrollmentDTO rejected = EnrollmentDTO.builder()
                    .id(1L)
                    .courseId(10)
                    .userId(studentUser.getId().toString())
                    .status(EnrollmentStatus.REJECTED)
                    .build();

            when(enrollmentService.validateEnrollment(
                    eq(1L), eq(EnrollmentStatus.REJECTED), eq(teacherUser.getId())))
                    .thenReturn(rejected);

            authenticateAs(teacherUser);
            mockMvc.perform(put("/api/enrollments/1/validate")
                            .param("status", "REJECTED"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REJECTED"));
        }
    }

    // =========================================================================
    // DÉSENRÔLEMENT
    // =========================================================================

    @Nested
    @DisplayName("Désenrôlement")
    class Unenroll {

        @Test
        @DisplayName("DELETE /api/enrollments/{id} - Succès")
        void unenroll_success() throws Exception {
            doNothing().when(enrollmentService).unenroll(eq(1L), eq(studentUser.getId()));

            authenticateAs(studentUser);
            mockMvc.perform(delete("/api/enrollments/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Désenrôlement réussi"));
        }

        @Test
        @DisplayName("DELETE /api/enrollments/{id} - Non trouvé (404)")
        void unenroll_notFound() throws Exception {
            doThrow(new ResourceNotFoundException("Enrôlement non trouvé"))
                    .when(enrollmentService).unenroll(eq(999L), any());

            authenticateAs(studentUser);
            mockMvc.perform(delete("/api/enrollments/999"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}