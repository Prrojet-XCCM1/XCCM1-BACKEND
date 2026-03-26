package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.CourseCreateRequest;
import com.ihm.backend.dto.request.CourseUpdateRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.CourseResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.CourseStatus;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.GlobalExceptionHandler;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.security.CustomAccessDeniedHandler;
import com.ihm.backend.security.JwtAuthenticationEntryPoint;
import com.ihm.backend.security.JwtAuthenticationFilter;
import com.ihm.backend.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(CourseController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    private ObjectMapper objectMapper;

    private User teacherUser;
    private final UUID teacherId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        teacherUser = User.builder()
                .id(teacherId)
                .email("teacher@test.com")
                .role(UserRole.TEACHER)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(teacherUser, null, List.of())
        );
    }

    private CourseResponse createSampleCourse(Integer id, String title) {
        CourseResponse res = new CourseResponse();
        res.setId(id);
        res.setTitle(title);
        return res;
    }

    @Nested
    @DisplayName("Gestion des Cours (Enseignant)")
    class TeacherOperations {
        @Test
        @DisplayName("POST /courses/{authorId} - Création réussie")
        void createCourse_success() throws Exception {
            CourseCreateRequest req = new CourseCreateRequest();
            req.setTitle("Spring Boot Expert");
            CourseResponse res = createSampleCourse(1, "Spring Boot Expert");

            when(courseService.createCourse(any(), eq(teacherId))).thenReturn(res);

            mockMvc.perform(post("/courses/" + teacherId)
                            .with(user(teacherUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.title").value("Spring Boot Expert"));
        }

        @Test
        @DisplayName("POST /courses/{authorId} - Accès refusé (403) pour un autre ID")
        void createCourse_forbidden() throws Exception {
            UUID otherId = UUID.randomUUID();
            CourseCreateRequest req = new CourseCreateRequest();

            mockMvc.perform(post("/courses/" + otherId)
                            .with(user(teacherUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /courses/{id} - Suppression réussie")
        void deleteCourse_success() throws Exception {
            mockMvc.perform(delete("/courses/1")
                            .with(user(teacherUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Cours supprimé avec succès"));
        }
        
        @Test
        @DisplayName("PUT /courses/{id} - Mise à jour réussie")
        void updateCourse_success() throws Exception {
            CourseUpdateRequest req = new CourseUpdateRequest();
            req.setTitle("Updated Title");
            CourseResponse res = createSampleCourse(1, "Updated Title");
            
            when(courseService.updateCourse(eq(1), any())).thenReturn(res);

            mockMvc.perform(put("/courses/1")
                            .with(user(teacherUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Updated Title"));
        }
    }

    @Nested
    @DisplayName("Opérations Publiques / Globales")
    class PublicAndGlobalOperations {
        @Test
        @DisplayName("GET /courses - Lister tous les cours")
        void getAllCourses_success() throws Exception {
            CourseResponse res = createSampleCourse(1, "Public Course");
            when(courseService.getAllCourses()).thenReturn(List.of(res));

            mockMvc.perform(get("/courses")
                            .with(user(teacherUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("GET /courses/enriched - Liste enrichie (Authentifié)")
        void getEnrichedCourses_success() throws Exception {
            when(courseService.getEnrichedCourses(teacherId)).thenReturn(List.of());

            mockMvc.perform(get("/courses/enriched")
                            .with(user(teacherUser)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /courses/{id}/view - Erreur 404")
        void incrementViewCount_notFound() throws Exception {
            when(courseService.incrementViewCount(999))
                    .thenThrow(new ResourceNotFoundException("Cours non trouvé"));

            mockMvc.perform(post("/courses/999/view")
                            .with(user(teacherUser)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("POST /courses/{id}/like - Erreur 500")
        void incrementLikeCount_internalError() throws Exception {
            when(courseService.incrementLikeCount(1))
                    .thenThrow(new RuntimeException("Erreur inattendue"));

            mockMvc.perform(post("/courses/1/like")
                            .with(user(teacherUser)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
