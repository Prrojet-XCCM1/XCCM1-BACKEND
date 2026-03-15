package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.CourseClassCreateRequest;
import com.ihm.backend.dto.request.CourseClassUpdateRequest;
import com.ihm.backend.dto.response.CourseClassResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.ClassStatus;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.service.CourseClassService;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour CourseClassController.
 *
 * Stratégie : standaloneSetup + RequestPostProcessor personnalisé qui injecte
 * le principal directement dans HttpServletRequest, ce que Spring MVC lit pour
 * résoudre le paramètre Authentication des méthodes du contrôleur.
 *
 * Endpoints testés (9) :
 *   POST   /api/classes                               createClass (TEACHER)
 *   GET    /api/classes/my                            getMyClasses (TEACHER)
 *   PUT    /api/classes/{classId}                     updateClass (TEACHER)
 *   DELETE /api/classes/{classId}                     deleteClass (TEACHER)
 *   PATCH  /api/classes/{classId}/status              changeStatus (TEACHER)
 *   POST   /api/classes/{classId}/courses/{courseId}  addCourse (TEACHER)
 *   DELETE /api/classes/{classId}/courses/{courseId}  removeCourse (TEACHER)
 *   GET    /api/classes                               getAllOpenClasses (public + étudiant)
 *   GET    /api/classes/{classId}                     getClassById (public + étudiant)
 */
@ExtendWith(MockitoExtension.class)
class CourseClassControllerTest {

    @Mock
    private CourseClassService courseClassService;

    @InjectMocks
    private CourseClassController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private User teacherUser;
    private User studentUser;
    private CourseClassResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        teacherUser = User.builder()
                .id(UUID.randomUUID())
                .email("teacher@test.com")
                .password("hashed")
                .role(UserRole.TEACHER)
                .build();

        studentUser = User.builder()
                .id(UUID.randomUUID())
                .email("student@test.com")
                .password("hashed")
                .role(UserRole.STUDENT)
                .build();

        sampleResponse = CourseClassResponse.builder()
                .id(1L)
                .name("Classe Java Avancé")
                .description("Cours avancés de Java")
                .theme("Informatique")
                .status(ClassStatus.OPEN)
                .studentCount(0L)
                .pendingCount(0L)
                .maxStudents(30)
                .build();
    }

    /**
     * Crée un RequestPostProcessor qui injecte un utilisateur authentifié
     * directement dans le HttpServletRequest. C'est la façon dont Spring MVC
     * résout le paramètre Authentication dans standaloneSetup.
     */
    private RequestPostProcessor asTeacher() {
        return (MockHttpServletRequest request) -> {
            request.setUserPrincipal(
                    new UsernamePasswordAuthenticationToken(teacherUser, null,
                            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
            return request;
        };
    }

    private RequestPostProcessor asStudent() {
        return (MockHttpServletRequest request) -> {
            request.setUserPrincipal(
                    new UsernamePasswordAuthenticationToken(studentUser, null,
                            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
            return request;
        };
    }

    // =========================================================================
    // POST /api/classes — createClass
    // =========================================================================
    @Nested
    @DisplayName("POST /api/classes — Créer une classe")
    class CreateClass {

        @Test
        @DisplayName("✅ TEACHER crée une classe → 201 Created")
        void teacher_canCreateClass() throws Exception {
            CourseClassCreateRequest req = new CourseClassCreateRequest();
            req.setName("Classe Java Avancé");
            req.setDescription("Cours avancés de Java");
            req.setTheme("Informatique");
            req.setMaxStudents(30);

            when(courseClassService.createClass(any(), eq(teacherUser.getId())))
                    .thenReturn(sampleResponse);

            mockMvc.perform(post("/api/classes")
                            .with(asTeacher())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name").value("Classe Java Avancé"))
                    .andExpect(jsonPath("$.data.status").value("OPEN"))
                    .andExpect(jsonPath("$.data.maxStudents").value(30));

            verify(courseClassService).createClass(any(), eq(teacherUser.getId()));
        }

        @Test
        @DisplayName("✅ Données minimales → 201 Created avec message de confirmation")
        void teacher_createClass_minimalData() throws Exception {
            CourseClassCreateRequest req = new CourseClassCreateRequest();
            req.setName("Classe Rapide");
            req.setMaxStudents(20);

            when(courseClassService.createClass(any(), eq(teacherUser.getId())))
                    .thenReturn(sampleResponse);

            mockMvc.perform(post("/api/classes")
                            .with(asTeacher())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Classe créée avec succès"));
        }
    }

    // =========================================================================
    // GET /api/classes/my — getMyClasses
    // =========================================================================
    @Nested
    @DisplayName("GET /api/classes/my — Mes classes (enseignant)")
    class GetMyClasses {

        @Test
        @DisplayName("✅ TEACHER récupère ses classes → 200 OK avec liste")
        void teacher_getsOwnClasses() throws Exception {
            when(courseClassService.getMyClasses(teacherUser.getId()))
                    .thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/classes/my").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Classe Java Avancé"));

            verify(courseClassService).getMyClasses(teacherUser.getId());
        }

        @Test
        @DisplayName("✅ TEACHER sans classes → 200 OK avec liste vide")
        void teacher_noClasses_returnsEmptyList() throws Exception {
            when(courseClassService.getMyClasses(teacherUser.getId())).thenReturn(List.of());

            mockMvc.perform(get("/api/classes/my").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // =========================================================================
    // PUT /api/classes/{classId} — updateClass
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/classes/{classId} — Modifier une classe")
    class UpdateClass {

        @Test
        @DisplayName("✅ TEACHER modifie le nom de sa classe → 200 OK")
        void teacher_canUpdateClass() throws Exception {
            CourseClassUpdateRequest req = new CourseClassUpdateRequest();
            req.setName("Nouveau Nom");

            CourseClassResponse updated = CourseClassResponse.builder()
                    .id(1L).name("Nouveau Nom").status(ClassStatus.OPEN)
                    .studentCount(0L).pendingCount(0L).build();

            when(courseClassService.updateClass(eq(1L), any(), eq(teacherUser.getId())))
                    .thenReturn(updated);

            mockMvc.perform(put("/api/classes/1")
                            .with(asTeacher())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Nouveau Nom"));

            verify(courseClassService).updateClass(eq(1L), any(), eq(teacherUser.getId()));
        }
    }

    // =========================================================================
    // DELETE /api/classes/{classId} — deleteClass
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/classes/{classId} — Supprimer une classe")
    class DeleteClass {

        @Test
        @DisplayName("✅ TEACHER supprime sa classe → 200 OK avec message de confirmation")
        void teacher_canDeleteClass() throws Exception {
            doNothing().when(courseClassService).deleteClass(eq(1L), eq(teacherUser.getId()));

            mockMvc.perform(delete("/api/classes/1").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Classe supprimée avec succès"));

            verify(courseClassService).deleteClass(1L, teacherUser.getId());
        }
    }

    // =========================================================================
    // PATCH /api/classes/{classId}/status — changeStatus
    // =========================================================================
    @Nested
    @DisplayName("PATCH /api/classes/{classId}/status — Changer le statut")
    class ChangeStatus {

        @Test
        @DisplayName("✅ TEACHER ferme sa classe → 200 OK, statut CLOSED")
        void teacher_canCloseClass() throws Exception {
            CourseClassResponse closed = CourseClassResponse.builder()
                    .id(1L).status(ClassStatus.CLOSED).studentCount(5L).pendingCount(0L).build();

            when(courseClassService.changeStatus(1L, ClassStatus.CLOSED, teacherUser.getId()))
                    .thenReturn(closed);

            mockMvc.perform(patch("/api/classes/1/status")
                            .with(asTeacher())
                            .param("status", "CLOSED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CLOSED"));
        }

        @Test
        @DisplayName("✅ TEACHER archive sa classe → 200 OK, statut ARCHIVED")
        void teacher_canArchiveClass() throws Exception {
            CourseClassResponse archived = CourseClassResponse.builder()
                    .id(1L).status(ClassStatus.ARCHIVED).studentCount(0L).pendingCount(0L).build();

            when(courseClassService.changeStatus(1L, ClassStatus.ARCHIVED, teacherUser.getId()))
                    .thenReturn(archived);

            mockMvc.perform(patch("/api/classes/1/status")
                            .with(asTeacher())
                            .param("status", "ARCHIVED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
        }

        @Test
        @DisplayName("✅ TEACHER rouvre sa classe → 200 OK, statut OPEN")
        void teacher_canReopenClass() throws Exception {
            when(courseClassService.changeStatus(1L, ClassStatus.OPEN, teacherUser.getId()))
                    .thenReturn(sampleResponse);

            mockMvc.perform(patch("/api/classes/1/status")
                            .with(asTeacher())
                            .param("status", "OPEN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("OPEN"));
        }
    }

    // =========================================================================
    // POST /api/classes/{classId}/courses/{courseId} — addCourse
    // =========================================================================
    @Nested
    @DisplayName("POST /api/classes/{classId}/courses/{courseId} — Ajouter un cours")
    class AddCourse {

        @Test
        @DisplayName("✅ TEACHER ajoute un cours à sa classe → 200 OK")
        void teacher_canAddCourse() throws Exception {
            when(courseClassService.addCourseToClass(1L, 42, teacherUser.getId()))
                    .thenReturn(sampleResponse);

            mockMvc.perform(post("/api/classes/1/courses/42").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Cours ajouté à la classe avec succès"));

            verify(courseClassService).addCourseToClass(1L, 42, teacherUser.getId());
        }
    }

    // =========================================================================
    // DELETE /api/classes/{classId}/courses/{courseId} — removeCourse
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/classes/{classId}/courses/{courseId} — Retirer un cours")
    class RemoveCourse {

        @Test
        @DisplayName("✅ TEACHER retire un cours de sa classe → 200 OK")
        void teacher_canRemoveCourse() throws Exception {
            when(courseClassService.removeCourseFromClass(1L, 42, teacherUser.getId()))
                    .thenReturn(sampleResponse);

            mockMvc.perform(delete("/api/classes/1/courses/42").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Cours retiré de la classe"));

            verify(courseClassService).removeCourseFromClass(1L, 42, teacherUser.getId());
        }
    }

    // =========================================================================
    // GET /api/classes — getAllOpenClasses
    // =========================================================================
    @Nested
    @DisplayName("GET /api/classes — Lister les classes ouvertes")
    class GetAllOpenClasses {

        @Test
        @DisplayName("✅ Accès anonyme → 200 OK avec liste des classes OPEN")
        void anonymous_canListOpenClasses() throws Exception {
            when(courseClassService.getAllOpenClasses(null)).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/classes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].status").value("OPEN"));

            verify(courseClassService).getAllOpenClasses(null);
        }

        @Test
        @DisplayName("✅ STUDENT connecté → service appelé avec UUID de l'étudiant")
        void student_getsEnrichedList() throws Exception {
            when(courseClassService.getAllOpenClasses(studentUser.getId()))
                    .thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/classes").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());

            verify(courseClassService).getAllOpenClasses(studentUser.getId());
        }

        @Test
        @DisplayName("✅ Aucune classe ouverte → 200 OK avec liste vide")
        void noOpenClasses_returnsEmpty() throws Exception {
            when(courseClassService.getAllOpenClasses(null)).thenReturn(List.of());

            mockMvc.perform(get("/api/classes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // =========================================================================
    // GET /api/classes/{classId} — getClassById
    // =========================================================================
    @Nested
    @DisplayName("GET /api/classes/{classId} — Détail d'une classe")
    class GetClassById {

        @Test
        @DisplayName("✅ Accès anonyme → 200 OK avec le détail complet")
        void anonymous_canGetClass() throws Exception {
            when(courseClassService.getClassById(1L, null)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/classes/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("Classe Java Avancé"));

            verify(courseClassService).getClassById(1L, null);
        }

        @Test
        @DisplayName("✅ STUDENT connecté → service appelé avec UUID de l'étudiant")
        void student_getsClassWithEnrollment() throws Exception {
            when(courseClassService.getClassById(1L, studentUser.getId()))
                    .thenReturn(sampleResponse);

            mockMvc.perform(get("/api/classes/1").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1));

            verify(courseClassService).getClassById(1L, studentUser.getId());
        }
    }
}
