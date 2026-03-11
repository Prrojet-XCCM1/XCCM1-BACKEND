package com.ihm.backend.controller;

import com.ihm.backend.dto.ClassEnrollmentDTO;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.EnrollmentStatus;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.service.ClassEnrollmentService;
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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour ClassEnrollmentController.
 *
 * Stratégie : standaloneSetup MockMvc + RequestPostProcessor personnalisé
 * qui injecte le principal dans HttpServletRequest (mécanisme natif Spring MVC).
 *
 * Endpoints testés (8) :
 *   POST   /api/class-enrollments/{classId}               enrollInClass (STUDENT)
 *   GET    /api/class-enrollments/my                      getMyEnrollments (STUDENT)
 *   GET    /api/class-enrollments/class/{classId}/me      getMyEnrollmentForClass (STUDENT)
 *   DELETE /api/class-enrollments/{enrollmentId}          unenrollFromClass (STUDENT)
 *   DELETE /api/class-enrollments/pending/{enrollmentId}  cancelPendingEnrollment (STUDENT)
 *   GET    /api/class-enrollments/pending                 getPendingEnrollments (TEACHER)
 *   GET    /api/class-enrollments/class/{classId}         getClassEnrollments (TEACHER)
 *   PUT    /api/class-enrollments/{enrollmentId}/validate validateEnrollment (TEACHER)
 */
@ExtendWith(MockitoExtension.class)
class ClassEnrollmentControllerTest {

    @Mock
    private ClassEnrollmentService enrollmentService;

    @InjectMocks
    private ClassEnrollmentController controller;

    private MockMvc mockMvc;

    private User studentUser;
    private User teacherUser;
    private ClassEnrollmentDTO sampleEnrollment;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        studentUser = User.builder()
                .id(UUID.randomUUID())
                .email("student@test.com")
                .password("hashed")
                .role(UserRole.STUDENT)
                .build();

        teacherUser = User.builder()
                .id(UUID.randomUUID())
                .email("teacher@test.com")
                .password("hashed")
                .role(UserRole.TEACHER)
                .build();

        sampleEnrollment = ClassEnrollmentDTO.builder()
                .id(10L)
                .classId(1L)
                .className("Classe Java Avancé")
                .studentId(studentUser.getId().toString())
                .studentName("Jean Dupont")
                .studentEmail("student@test.com")
                .status(EnrollmentStatus.PENDING)
                .enrolledAt(LocalDateTime.now())
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
    // POST /api/class-enrollments/{classId} — enrollInClass
    // =========================================================================
    @Nested
    @DisplayName("POST /api/class-enrollments/{classId} — S'inscrire à une classe")
    class EnrollInClass {

        @Test
        @DisplayName("✅ STUDENT s'inscrit → 200 OK, statut PENDING")
        void student_canEnroll() throws Exception {
            when(enrollmentService.enrollInClass(eq(1L), eq(studentUser.getId())))
                    .thenReturn(sampleEnrollment);

            mockMvc.perform(post("/api/class-enrollments/1").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.classId").value(1))
                    .andExpect(jsonPath("$.data.studentName").value("Jean Dupont"));

            verify(enrollmentService).enrollInClass(1L, studentUser.getId());
        }

        @Test
        @DisplayName("✅ Classe déjà complète → service lève IllegalStateException")
        void classIsFull_serviceThrowsException() throws Exception {
            when(enrollmentService.enrollInClass(eq(1L), eq(studentUser.getId())))
                    .thenThrow(new IllegalStateException("La classe est complète"));

            // MockMvc propage l'exception native → on utilise assertThrows
            org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                    mockMvc.perform(post("/api/class-enrollments/1").with(asStudent())).andReturn());

            verify(enrollmentService).enrollInClass(1L, studentUser.getId());
        }

        @Test
        @DisplayName("✅ Déjà inscrit → service lève IllegalStateException")
        void alreadyEnrolled_serviceThrowsException() throws Exception {
            when(enrollmentService.enrollInClass(eq(1L), eq(studentUser.getId())))
                    .thenThrow(new IllegalStateException("Vous êtes déjà inscrit"));

            org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                    mockMvc.perform(post("/api/class-enrollments/1").with(asStudent())).andReturn());

            verify(enrollmentService).enrollInClass(1L, studentUser.getId());
        }
    }

    // =========================================================================
    // GET /api/class-enrollments/my — getMyEnrollments
    // =========================================================================
    @Nested
    @DisplayName("GET /api/class-enrollments/my — Mes inscriptions")
    class GetMyEnrollments {

        @Test
        @DisplayName("✅ STUDENT récupère ses inscriptions → 200 OK avec liste")
        void student_getsOwnEnrollments() throws Exception {
            when(enrollmentService.getMyEnrollments(studentUser.getId()))
                    .thenReturn(List.of(sampleEnrollment));

            mockMvc.perform(get("/api/class-enrollments/my").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(10))
                    .andExpect(jsonPath("$.data[0].status").value("PENDING"));

            verify(enrollmentService).getMyEnrollments(studentUser.getId());
        }

        @Test
        @DisplayName("✅ STUDENT sans inscriptions → 200 OK avec liste vide")
        void student_noEnrollments_returnsEmptyList() throws Exception {
            when(enrollmentService.getMyEnrollments(studentUser.getId())).thenReturn(List.of());

            mockMvc.perform(get("/api/class-enrollments/my").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // =========================================================================
    // GET /api/class-enrollments/class/{classId}/me
    // =========================================================================
    @Nested
    @DisplayName("GET /api/class-enrollments/class/{classId}/me — Mon inscription à une classe")
    class GetMyEnrollmentForClass {

        @Test
        @DisplayName("✅ STUDENT inscrit → 200 OK avec son enrollment")
        void student_enrolled_returnsEnrollment() throws Exception {
            when(enrollmentService.getEnrollmentForClass(1L, studentUser.getId()))
                    .thenReturn(sampleEnrollment);

            mockMvc.perform(get("/api/class-enrollments/class/1/me").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(10))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("✅ STUDENT non inscrit → 200 OK avec message explicite")
        void student_notEnrolled_returnsNullData() throws Exception {
            when(enrollmentService.getEnrollmentForClass(1L, studentUser.getId())).thenReturn(null);

            mockMvc.perform(get("/api/class-enrollments/class/1/me").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Non inscrit à cette classe"));
        }
    }

    // =========================================================================
    // DELETE /api/class-enrollments/{enrollmentId} — unenrollFromClass
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/class-enrollments/{enrollmentId} — Se désinscrire")
    class UnenrollFromClass {

        @Test
        @DisplayName("✅ STUDENT se désinscrit → 200 OK")
        void student_canUnenroll() throws Exception {
            doNothing().when(enrollmentService).unenrollFromClass(10L, studentUser.getId());

            mockMvc.perform(delete("/api/class-enrollments/10").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Désinscription réussie"));

            verify(enrollmentService).unenrollFromClass(10L, studentUser.getId());
        }

        @Test
        @DisplayName("✅ Inscription non trouvée → ResourceNotFoundException → HTTP 404")
        void enrollmentNotFound_serviceThrows() throws Exception {
            doThrow(new com.ihm.backend.exception.ResourceNotFoundException("Inscription non trouvée: 999"))
                    .when(enrollmentService).unenrollFromClass(999L, studentUser.getId());

            // @ResponseStatus(NOT_FOUND) sur ResourceNotFoundException → MockMvc retourne 404
            mockMvc.perform(delete("/api/class-enrollments/999").with(asStudent()))
                    .andExpect(status().isNotFound());

            verify(enrollmentService).unenrollFromClass(999L, studentUser.getId());
        }
    }

    // =========================================================================
    // DELETE /api/class-enrollments/pending/{enrollmentId}
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/class-enrollments/pending/{enrollmentId} — Annuler PENDING")
    class CancelPendingEnrollment {

        @Test
        @DisplayName("✅ STUDENT annule sa demande PENDING → 200 OK")
        void student_canCancelPending() throws Exception {
            doNothing().when(enrollmentService).cancelPendingEnrollment(10L, studentUser.getId());

            mockMvc.perform(delete("/api/class-enrollments/pending/10").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Demande d'inscription annulée"));

            verify(enrollmentService).cancelPendingEnrollment(10L, studentUser.getId());
        }

        @Test
        @DisplayName("✅ Inscription pas PENDING → service lève IllegalStateException")
        void notPending_serviceThrows() throws Exception {
            doThrow(new IllegalStateException("Seules les inscriptions PENDING peuvent être annulées"))
                    .when(enrollmentService).cancelPendingEnrollment(10L, studentUser.getId());

            org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                    mockMvc.perform(delete("/api/class-enrollments/pending/10").with(asStudent())).andReturn());

            verify(enrollmentService).cancelPendingEnrollment(10L, studentUser.getId());
        }
    }

    // =========================================================================
    // GET /api/class-enrollments/pending — getPendingEnrollments (TEACHER)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/class-enrollments/pending — Demandes en attente (enseignant)")
    class GetPendingEnrollments {

        @Test
        @DisplayName("✅ TEACHER récupère les inscriptions PENDING → 200 OK")
        void teacher_getsPendingEnrollments() throws Exception {
            when(enrollmentService.getPendingForTeacher(teacherUser.getId()))
                    .thenReturn(List.of(sampleEnrollment));

            mockMvc.perform(get("/api/class-enrollments/pending").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].status").value("PENDING"));

            verify(enrollmentService).getPendingForTeacher(teacherUser.getId());
        }

        @Test
        @DisplayName("✅ TEACHER sans demandes → 200 OK avec liste vide")
        void teacher_noPending_returnsEmpty() throws Exception {
            when(enrollmentService.getPendingForTeacher(teacherUser.getId())).thenReturn(List.of());

            mockMvc.perform(get("/api/class-enrollments/pending").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // =========================================================================
    // GET /api/class-enrollments/class/{classId} — getClassEnrollments (TEACHER)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/class-enrollments/class/{classId} — Inscrits d'une classe (enseignant)")
    class GetClassEnrollments {

        @Test
        @DisplayName("✅ TEACHER récupère tous les inscrits → 200 OK avec 2 étudiants")
        void teacher_getsAllEnrollments() throws Exception {
            ClassEnrollmentDTO approved = ClassEnrollmentDTO.builder()
                    .id(11L).classId(1L).status(EnrollmentStatus.APPROVED)
                    .studentName("Alice Martin").build();

            when(enrollmentService.getClassEnrollments(1L, teacherUser.getId()))
                    .thenReturn(List.of(sampleEnrollment, approved));

            mockMvc.perform(get("/api/class-enrollments/class/1").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2));

            verify(enrollmentService).getClassEnrollments(1L, teacherUser.getId());
        }
    }

    // =========================================================================
    // PUT /api/class-enrollments/{enrollmentId}/validate — validateEnrollment
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/class-enrollments/{enrollmentId}/validate — Valider/Rejeter")
    class ValidateEnrollment {

        @Test
        @DisplayName("✅ TEACHER approuve → 200 OK, message de succès")
        void teacher_canApproveEnrollment() throws Exception {
            ClassEnrollmentDTO approved = ClassEnrollmentDTO.builder()
                    .id(10L).classId(1L).status(EnrollmentStatus.APPROVED)
                    .studentName("Jean Dupont").build();

            when(enrollmentService.validateEnrollment(10L, EnrollmentStatus.APPROVED, teacherUser.getId()))
                    .thenReturn(approved);

            mockMvc.perform(put("/api/class-enrollments/10/validate")
                            .with(asTeacher())
                            .param("status", "APPROVED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andExpect(jsonPath("$.message").value("Étudiant validé avec succès"));

            verify(enrollmentService).validateEnrollment(10L, EnrollmentStatus.APPROVED, teacherUser.getId());
        }

        @Test
        @DisplayName("✅ TEACHER rejette → 200 OK, message de refus")
        void teacher_canRejectEnrollment() throws Exception {
            ClassEnrollmentDTO rejected = ClassEnrollmentDTO.builder()
                    .id(10L).classId(1L).status(EnrollmentStatus.REJECTED)
                    .studentName("Jean Dupont").build();

            when(enrollmentService.validateEnrollment(10L, EnrollmentStatus.REJECTED, teacherUser.getId()))
                    .thenReturn(rejected);

            mockMvc.perform(put("/api/class-enrollments/10/validate")
                            .with(asTeacher())
                            .param("status", "REJECTED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REJECTED"))
                    .andExpect(jsonPath("$.message").value("Étudiant refusé"));
        }

        @Test
        @DisplayName("✅ TEACHER non-propriétaire → service lève AccessDeniedException")
        void nonOwnerTeacher_serviceThrows() throws Exception {
            when(enrollmentService.validateEnrollment(10L, EnrollmentStatus.APPROVED, teacherUser.getId()))
                    .thenThrow(new org.springframework.security.access.AccessDeniedException("Pas votre classe"));

            org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                    mockMvc.perform(put("/api/class-enrollments/10/validate")
                            .with(asTeacher())
                            .param("status", "APPROVED")).andReturn());

            verify(enrollmentService).validateEnrollment(10L, EnrollmentStatus.APPROVED, teacherUser.getId());
        }
    }
}
