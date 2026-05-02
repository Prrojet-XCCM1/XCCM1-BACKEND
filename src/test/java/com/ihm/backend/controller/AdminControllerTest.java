package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.*;
import com.ihm.backend.dto.response.*;
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
import com.ihm.backend.service.AdminService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests unitaires pour AdminController.
 * Refactorisés selon les standards experts Spring Boot.
 */
@WebMvcTest(AdminController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

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

    private User adminUser;
    private final UUID randomId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, List.of())
        );
    }

    @Nested
    @DisplayName("Statistiques d'Administration")
    class Statistics {
        @Test
        @DisplayName("GET /api/v1/admin/stats - Succès")
        void getStatistics_success() throws Exception {
            AdminStatisticsResponse stats = AdminStatisticsResponse.builder()
                    .totalUsers(10L).studentCount(5L).teacherCount(4L).totalEnrollments(15L)
                    .build();
            when(adminService.getStatistics()).thenReturn(ApiResponse.success("Succès", stats));

            mockMvc.perform(get("/api/v1/admin/stats"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalUsers").value(10));
        }

        @Test
        @DisplayName("GET /api/v1/admin/enrollments/stats - Erreur Interne (500)")
        void getEnrollmentStatistics_error() throws Exception {
            when(adminService.getEnrollmentStatistics())
                    .thenThrow(new RuntimeException("Erreur inattendue"));

            mockMvc.perform(get("/api/v1/admin/enrollments/stats"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("Gestion des Utilisateurs")
    class UserManagement {
        @Test
        @DisplayName("POST /api/v1/admin/users/student - Création réussie")
        void createStudent_success() throws Exception {
            StudentRegisterRequest req = new StudentRegisterRequest();
            req.setEmail("new@student.com");
            AuthenticationResponse authRes = AuthenticationResponse.builder().token("jwt-token").build();
            
            when(adminService.createStudent(any())).thenReturn(ApiResponse.created("Succès", authRes));

            mockMvc.perform(post("/api/v1/admin/users/student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.token").value("jwt-token"));
        }

        @Test
        @DisplayName("GET /api/v1/admin/users/{id} - Utilisateur non trouvé (404)")
        void getUserById_notFound() throws Exception {
            when(adminService.getUserById(randomId))
                    .thenThrow(new ResourceNotFoundException("Utilisateur non trouvé"));

            mockMvc.perform(get("/api/v1/admin/users/" + randomId))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("DELETE /api/v1/admin/users/{id} - Suppression réussie")
        void deleteUser_success() throws Exception {
            when(adminService.deleteUser(randomId)).thenReturn(ApiResponse.success("User deleted"));

            mockMvc.perform(delete("/api/v1/admin/users/" + randomId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User deleted"));
        }
    }

    @Nested
    @DisplayName("Gestion des Cours")
    class CourseManagement {
        @Test
        @DisplayName("GET /api/v1/admin/courses - Liste des cours")
        void getAllCourses_success() throws Exception {
            CourseResponse course = new CourseResponse();
            course.setId(1);
            course.setTitle("Spring Boot Expert");
            when(adminService.getAllCourses()).thenReturn(ApiResponse.success("Succès", List.of(course)));

            mockMvc.perform(get("/api/v1/admin/courses"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].title").value("Spring Boot Expert"));
        }
    }
}
