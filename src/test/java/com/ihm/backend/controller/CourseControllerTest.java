package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.CourseCreateRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.CourseResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.service.CourseService;
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
class CourseControllerTest {

    @Mock
    private CourseService courseService;

    @InjectMocks
    private CourseController courseController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private User teacherUser;
    private UUID teacherId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(courseController).build();

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

    private CourseResponse createSampleCourse(Integer id, String title) {
        CourseResponse res = new CourseResponse();
        res.setId(id);
        res.setTitle(title);
        return res;
    }

    @Nested
    @DisplayName("Gestion des Cours (TEACHER)")
    class TeacherOperations {
        @Test
        @DisplayName("POST /courses/{authorId} - Créer un cours success")
        void createCourse_success() throws Exception {
            CourseCreateRequest req = new CourseCreateRequest();
            req.setTitle("Spring Boot Test");
            CourseResponse res = createSampleCourse(1, "Spring Boot Test");

            when(courseService.createCourse(any(), eq(teacherId))).thenReturn(res);

            mockMvc.perform(post("/courses/" + teacherId)
                            .with(asTeacher())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.title").value("Spring Boot Test"));
        }

        @Test
        @DisplayName("POST /courses/{authorId} - Créer un cours forbidden (auto-création uniquement)")
        void createCourse_forbidden() throws Exception {
            UUID otherId = UUID.randomUUID();
            CourseCreateRequest req = new CourseCreateRequest();

            mockMvc.perform(post("/courses/" + otherId)
                            .with(asTeacher())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /courses/{authorId} - Mes cours")
        void getAuthorCourses_success() throws Exception {
            CourseResponse res = createSampleCourse(1, "Test Course");
            when(courseService.getAllCoursesForTeacher(teacherId)).thenReturn(List.of(res));

            mockMvc.perform(get("/courses/" + teacherId).with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("DELETE /courses/{courseId} - Supprimer cours")
        void deleteCourse_success() throws Exception {
            mockMvc.perform(delete("/courses/1").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Cours supprimé avec succès"));
        }
    }

    @Nested
    @DisplayName("Opérations Publiques")
    class PublicOperations {
        @Test
        @DisplayName("GET /courses - Lister tous les cours")
        void getAllCourses_success() throws Exception {
            CourseResponse res = createSampleCourse(1, "Public Course");
            when(courseService.getAllCourses()).thenReturn(List.of(res));

            mockMvc.perform(get("/courses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("POST /courses/{id}/view - Incrémenter les vues")
        void incrementViewCount_success() throws Exception {
            CourseResponse res = createSampleCourse(1, "Test");
            res.setViewCount(101L);
            when(courseService.incrementViewCount(1)).thenReturn(res);

            mockMvc.perform(post("/courses/1/view"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.viewCount").value(101));
        }
    }
}
