package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.CourseClassCreateRequest;
import com.ihm.backend.dto.request.CourseClassUpdateRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.CourseClassResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.ClassStatus;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.GlobalExceptionHandler;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.security.CustomAccessDeniedHandler;
import com.ihm.backend.security.JwtAuthenticationEntryPoint;
import com.ihm.backend.security.JwtAuthenticationFilter;
import com.ihm.backend.service.CourseClassService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(CourseClassController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseClassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseClassService courseClassService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    private ObjectMapper objectMapper;

    private User teacherUser;
    private User studentUser;
    private final UUID teacherId = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private final UUID studentId = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private CourseClassResponse sampleResponse;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder()
                .id(teacherId)
                .email("teacher@test.com")
                .role(UserRole.TEACHER)
                .build();

        studentUser = User.builder()
                .id(studentId)
                .email("student@test.com")
                .role(UserRole.STUDENT)
                .build();

        sampleResponse = CourseClassResponse.builder()
                .id(1L)
                .name("Classe Java Avancé")
                .status(ClassStatus.OPEN)
                .build();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }

    @Nested
    @DisplayName("Opérations Enseignant")
    class TeacherOperations {
        @Test
        @DisplayName("POST /api/classes - Création réussie")
        void createClass_success() throws Exception {
            authenticateAs(teacherUser);
            CourseClassCreateRequest req = new CourseClassCreateRequest();
            req.setName("Classe Java Avancé");

            when(courseClassService.createClass(any(), eq(teacherId))).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/classes")
                            .with(user(teacherUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name").value("Classe Java Avancé"));
        }

        @Test
        @DisplayName("PUT /api/classes/{id} - Mise à jour réussie")
        void updateClass_success() throws Exception {
            authenticateAs(teacherUser);
            CourseClassUpdateRequest req = new CourseClassUpdateRequest();
            req.setName("Nouveau Nom");

            when(courseClassService.updateClass(eq(1L), any(), eq(teacherId))).thenReturn(sampleResponse);

            mockMvc.perform(put("/api/classes/1")
                            .with(user(teacherUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE /api/classes/{id} - Suppression réussie")
        void deleteClass_success() throws Exception {
            authenticateAs(teacherUser);
            doNothing().when(courseClassService).deleteClass(eq(1L), eq(teacherId));

            mockMvc.perform(delete("/api/classes/1")
                            .with(user(teacherUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Classe supprimée avec succès"));
        }
    }

    @Nested
    @DisplayName("Opérations Publiques / Étudiant")
    class PublicOperations {
        @Test
        @DisplayName("GET /api/classes - Liste des classes ouvertes")
        void getAllOpenClasses_success() throws Exception {
            authenticateAs(studentUser);
            when(courseClassService.getAllOpenClasses(eq(studentId))).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/classes")
                            .with(user(studentUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].status").value("OPEN"));
        }

        @Test
        @DisplayName("GET /api/classes/{id} - Détail (404)")
        void getClassById_notFound() throws Exception {
            when(courseClassService.getClassById(eq(999L), any()))
                    .thenThrow(new ResourceNotFoundException("Classe non trouvée"));

            mockMvc.perform(get("/api/classes/999")
                            .with(user(studentUser)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
