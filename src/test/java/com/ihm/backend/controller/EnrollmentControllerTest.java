package com.ihm.backend.controller;

import com.ihm.backend.dto.EnrollmentDTO;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.EnrollmentStatus;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.service.EnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour EnrollmentController.
 *
 * Stratégie : standaloneSetup MockMvc + RequestPostProcessor personnalisé.
 *
 * Scénarios couverts :
 *   - Un STUDENT s'enrôle → status PENDING
 *   - Un TEACHER s'enrôle → status APPROVED (auto-approuvé)
 *   - Doublon → IllegalStateException
 *   - Un TEACHER consulte ses cours enrôlés
 *   - Un TEACHER valide/rejette un enrôlement étudiant
 *   - Désenrôlement et annulation
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentControllerTest {

    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private EnrollmentController controller;

    private MockMvc mockMvc;

    private User studentUser;
    private User teacherUser;
    private EnrollmentDTO studentEnrollment;
    private EnrollmentDTO teacherEnrollment;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        studentUser = User.builder()
                .id(UUID.randomUUID())
                .email("student@test.com")
                .password("hashed")
                .role(UserRole.STUDENT)
                .firstName("Jean")
                .lastName("Dupont")
                .build();

        teacherUser = User.builder()
                .id(UUID.randomUUID())
                .email("teacher@test.com")
                .password("hashed")
                .role(UserRole.TEACHER)
                .firstName("Marie")
                .lastName("Curie")
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
                .status(EnrollmentStatus.APPROVED) // auto-approuvé
                .build();
    }

    private RequestPostProcessor asStudent() {
        return (MockHttpServletRequest request) -> {
            request.setUserPrincipal(
                    new UsernamePasswordAuthenticationToken(studentUser, null,
                            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
            return request;
        };
    }

    private RequestPostProcessor asTeacher() {
        return (MockHttpServletRequest request) -> {
            request.setUserPrincipal(
                    new UsernamePasswordAuthenticationToken(teacherUser, null,
                            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
            return request;
        };
    }

    // =========================================================================
    // POST /api/enrollments/courses/{courseId} — enrollInCourse
    // =========================================================================
    @Nested
    @DisplayName("POST /api/enrollments/courses/{courseId} — S'enrôler à un cours")
    class EnrollInCourse {

        @Test
        @DisplayName("✅ STUDENT s'enrôle → 200 OK, statut PENDING")
        void student_canEnroll_statusIsPending() throws Exception {
            when(enrollmentService.enrollUser(eq(10), eq(studentUser.getId())))
                    .thenReturn(studentEnrollment);

            mockMvc.perform(post("/api/enrollments/courses/10").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.courseId").value(10))
                    .andExpect(jsonPath("$.message").value("Enrôlement réussi"));

            verify(enrollmentService).enrollUser(10, studentUser.getId());
        }

        @Test
        @DisplayName("✅ TEACHER s'enrôle → 200 OK, statut APPROVED (auto-approuvé)")
        void teacher_canEnroll_statusIsApproved() throws Exception {
            when(enrollmentService.enrollUser(eq(10), eq(teacherUser.getId())))
                    .thenReturn(teacherEnrollment);

            mockMvc.perform(post("/api/enrollments/courses/10").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andExpect(jsonPath("$.message").value("Enrôlement réussi"));

            verify(enrollmentService).enrollUser(10, teacherUser.getId());
        }

        @Test
        @DisplayName("✅ Cours déjà enrôlé → service lève IllegalStateException")
        void alreadyEnrolled_serviceThrows() throws Exception {
            when(enrollmentService.enrollUser(eq(10), eq(studentUser.getId())))
                    .thenThrow(new IllegalStateException("Vous êtes déjà enrôlé à ce cours"));

            assertThrows(Exception.class, () ->
                    mockMvc.perform(post("/api/enrollments/courses/10").with(asStudent())).andReturn());

            verify(enrollmentService).enrollUser(10, studentUser.getId());
        }

        @Test
        @DisplayName("✅ Cours non publié → service lève IllegalStateException")
        void courseNotPublished_serviceThrows() throws Exception {
            when(enrollmentService.enrollUser(eq(10), eq(teacherUser.getId())))
                    .thenThrow(new IllegalStateException("Ce cours n'est pas encore publié"));

            assertThrows(Exception.class, () ->
                    mockMvc.perform(post("/api/enrollments/courses/10").with(asTeacher())).andReturn());
        }

        @Test
        @DisplayName("✅ TEACHER enrôlé à son propre cours → service lève AccessDeniedException")
        void teacher_enrollInOwnCourse_serviceThrows() throws Exception {
            when(enrollmentService.enrollUser(eq(10), eq(teacherUser.getId())))
                    .thenThrow(new java.nio.file.AccessDeniedException(
                            "Un enseignant ne peut pas s'enrôler à son propre cours"));

            assertThrows(Exception.class, () ->
                    mockMvc.perform(post("/api/enrollments/courses/10").with(asTeacher())).andReturn());
        }
    }

    // =========================================================================
    // GET /api/enrollments/my-courses
    // =========================================================================
    @Nested
    @DisplayName("GET /api/enrollments/my-courses — Mes cours enrôlés")
    class GetMyEnrollments {

        @Test
        @DisplayName("✅ STUDENT récupère ses enrôlements → 200 OK")
        void student_getsOwnEnrollments() throws Exception {
            when(enrollmentService.getUserEnrollments(studentUser.getId()))
                    .thenReturn(List.of(studentEnrollment));

            mockMvc.perform(get("/api/enrollments/my-courses").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                    .andExpect(jsonPath("$.data[0].courseId").value(10));

            verify(enrollmentService).getUserEnrollments(studentUser.getId());
        }

        @Test
        @DisplayName("✅ TEACHER récupère ses enrôlements → 200 OK avec statut APPROVED")
        void teacher_getsOwnEnrollments() throws Exception {
            when(enrollmentService.getUserEnrollments(teacherUser.getId()))
                    .thenReturn(List.of(teacherEnrollment));

            mockMvc.perform(get("/api/enrollments/my-courses").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].status").value("APPROVED"));

            verify(enrollmentService).getUserEnrollments(teacherUser.getId());
        }

        @Test
        @DisplayName("✅ Aucun enrôlement → 200 OK avec liste vide")
        void noEnrollments_returnsEmptyList() throws Exception {
            when(enrollmentService.getUserEnrollments(studentUser.getId())).thenReturn(List.of());

            mockMvc.perform(get("/api/enrollments/my-courses").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // =========================================================================
    // GET /api/enrollments/courses/{courseId} — vérifier enrôlement
    // =========================================================================
    @Nested
    @DisplayName("GET /api/enrollments/courses/{courseId} — Statut d'enrôlement")
    class GetEnrollmentForCourse {

        @Test
        @DisplayName("✅ STUDENT enrôlé → 200 OK avec l'enrôlement")
        void student_enrolled_returnsEnrollment() throws Exception {
            when(enrollmentService.getEnrollmentForUser(10, studentUser.getId()))
                    .thenReturn(studentEnrollment);

            mockMvc.perform(get("/api/enrollments/courses/10").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("✅ TEACHER enrôlé → 200 OK avec statut APPROVED")
        void teacher_enrolled_returnsEnrollmentApproved() throws Exception {
            when(enrollmentService.getEnrollmentForUser(10, teacherUser.getId()))
                    .thenReturn(teacherEnrollment);

            mockMvc.perform(get("/api/enrollments/courses/10").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("APPROVED"));
        }

        @Test
        @DisplayName("✅ Non enrôlé → 200 OK avec data null")
        void notEnrolled_returnsNull() throws Exception {
            when(enrollmentService.getEnrollmentForUser(10, studentUser.getId())).thenReturn(null);

            mockMvc.perform(get("/api/enrollments/courses/10").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Non enrôlé"));
        }
    }

    // =========================================================================
    // PUT /api/enrollments/{enrollmentId}/validate — TEACHER valide
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/enrollments/{enrollmentId}/validate — Valider un enrôlement")
    class ValidateEnrollment {

        @Test
        @DisplayName("✅ TEACHER approuve → 200 OK, statut APPROVED")
        void teacher_canApprove() throws Exception {
            EnrollmentDTO approved = EnrollmentDTO.builder()
                    .id(1L)
                    .courseId(10)
                    .status(EnrollmentStatus.APPROVED)
                    .build();

            when(enrollmentService.validateEnrollment(1L, EnrollmentStatus.APPROVED, teacherUser.getId()))
                    .thenReturn(approved);

            mockMvc.perform(put("/api/enrollments/1/validate")
                            .with(asTeacher())
                            .param("status", "APPROVED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("APPROVED"));

            verify(enrollmentService).validateEnrollment(1L, EnrollmentStatus.APPROVED, teacherUser.getId());
        }

        @Test
        @DisplayName("✅ TEACHER rejette → 200 OK, statut REJECTED")
        void teacher_canReject() throws Exception {
            EnrollmentDTO rejected = EnrollmentDTO.builder()
                    .id(1L)
                    .courseId(10)
                    .status(EnrollmentStatus.REJECTED)
                    .build();

            when(enrollmentService.validateEnrollment(1L, EnrollmentStatus.REJECTED, teacherUser.getId()))
                    .thenReturn(rejected);

            mockMvc.perform(put("/api/enrollments/1/validate")
                            .with(asTeacher())
                            .param("status", "REJECTED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REJECTED"));
        }
    }

    // =========================================================================
    // GET /api/enrollments/pending — TEACHER → liste des PENDING
    // =========================================================================
    @Nested
    @DisplayName("GET /api/enrollments/pending — Enrôlements en attente (enseignant)")
    class GetPendingEnrollments {

        @Test
        @DisplayName("✅ TEACHER récupère les PENDING → 200 OK")
        void teacher_getsPendingEnrollments() throws Exception {
            when(enrollmentService.getPendingEnrollmentsForTeacher(teacherUser.getId()))
                    .thenReturn(List.of(studentEnrollment));

            mockMvc.perform(get("/api/enrollments/pending").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].status").value("PENDING"));

            verify(enrollmentService).getPendingEnrollmentsForTeacher(teacherUser.getId());
        }

        @Test
        @DisplayName("✅ Aucun PENDING → 200 OK avec liste vide")
        void teacher_noPending_returnsEmpty() throws Exception {
            when(enrollmentService.getPendingEnrollmentsForTeacher(teacherUser.getId()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/enrollments/pending").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // =========================================================================
    // DELETE /api/enrollments/{enrollmentId} — désenrôlement
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/enrollments/{enrollmentId} — Se désenrôler")
    class Unenroll {

        @Test
        @DisplayName("✅ STUDENT se désenrôle → 200 OK")
        void student_canUnenroll() throws Exception {
            doNothing().when(enrollmentService).unenroll(1L, studentUser.getId());

            mockMvc.perform(delete("/api/enrollments/1").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Désenrôlement réussi"));

            verify(enrollmentService).unenroll(1L, studentUser.getId());
        }

        @Test
        @DisplayName("✅ TEACHER se désenrôle → 200 OK")
        void teacher_canUnenroll() throws Exception {
            doNothing().when(enrollmentService).unenroll(2L, teacherUser.getId());

            mockMvc.perform(delete("/api/enrollments/2").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Désenrôlement réussi"));

            verify(enrollmentService).unenroll(2L, teacherUser.getId());
        }

        @Test
        @DisplayName("✅ Enrôlement introuvable → ResourceNotFoundException → 404")
        void enrollmentNotFound_returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Enrôlement non trouvé"))
                    .when(enrollmentService).unenroll(999L, studentUser.getId());

            mockMvc.perform(delete("/api/enrollments/999").with(asStudent()))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // DELETE /api/enrollments/pending/{enrollmentId} — annuler PENDING
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/enrollments/pending/{enrollmentId} — Annuler PENDING")
    class CancelPending {

        @Test
        @DisplayName("✅ STUDENT annule sa demande PENDING → 200 OK")
        void student_cancelsPending() throws Exception {
            doNothing().when(enrollmentService).cancelPendingEnrollment(1L, studentUser.getId());

            mockMvc.perform(delete("/api/enrollments/pending/1").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Demande d'enrôlement annulée"));

            verify(enrollmentService).cancelPendingEnrollment(1L, studentUser.getId());
        }

        @Test
        @DisplayName("✅ TEACHER annule son enrôlement PENDING → 200 OK")
        void teacher_cancelsPending() throws Exception {
            doNothing().when(enrollmentService).cancelPendingEnrollment(2L, teacherUser.getId());

            mockMvc.perform(delete("/api/enrollments/pending/2").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Demande d'enrôlement annulée"));
        }

        @Test
        @DisplayName("✅ Statut pas PENDING → service lève IllegalStateException")
        void notPending_serviceThrows() throws Exception {
            doThrow(new IllegalStateException("Seuls les enrôlements en attente peuvent être annulés"))
                    .when(enrollmentService).cancelPendingEnrollment(1L, studentUser.getId());

            assertThrows(Exception.class, () ->
                    mockMvc.perform(delete("/api/enrollments/pending/1").with(asStudent())).andReturn());
        }
    }
}
