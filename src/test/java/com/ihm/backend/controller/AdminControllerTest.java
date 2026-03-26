package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.*;
import com.ihm.backend.dto.response.*;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.service.AdminService;
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
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private User adminUser;
    private UUID randomId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .build();
    }

    private RequestPostProcessor asAdmin() {
        return (MockHttpServletRequest request) -> {
            request.setUserPrincipal(
                    new UsernamePasswordAuthenticationToken(adminUser, null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
            return request;
        };
    }

    @Nested
    @DisplayName("Statistiques")
    class Statistics {
        @Test
        @DisplayName("GET /api/v1/admin/stats - Récupérer les stats globales")
        void getStatistics_returnsStats() throws Exception {
            AdminStatisticsResponse stats = AdminStatisticsResponse.builder()
                    .totalUsers(10L).studentCount(5L).teacherCount(4L).totalEnrollments(15L)
                    .build();
            when(adminService.getStatistics()).thenReturn(ApiResponse.success("Success", stats));

            mockMvc.perform(get("/api/v1/admin/stats").with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalUsers").value(10));
        }

        @Test
        @DisplayName("GET /api/v1/admin/enrollments/stats - Récupérer les stats enrollments")
        void getEnrollmentStatistics_returnsStats() throws Exception {
            EnrollmentStatsResponse stats = EnrollmentStatsResponse.builder().totalEnrollments(50L).build();
            when(adminService.getEnrollmentStatistics()).thenReturn(ApiResponse.success("Success", stats));

            mockMvc.perform(get("/api/v1/admin/enrollments/stats").with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalEnrollments").value(50));
        }
    }

    @Nested
    @DisplayName("Gestion des Utilisateurs")
    class UserManagement {
        @Test
        @DisplayName("POST /api/v1/admin/users/student - Créer étudiant")
        void createStudent_callsService() throws Exception {
            StudentRegisterRequest req = new StudentRegisterRequest();
            req.setEmail("new@student.com");
            AuthenticationResponse authRes = AuthenticationResponse.builder().token("jwt").build();
            when(adminService.createStudent(any())).thenReturn(ApiResponse.created("success", authRes));

            mockMvc.perform(post("/api/v1/admin/users/student")
                            .with(asAdmin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.token").value("jwt"));
        }

        @Test
        @DisplayName("GET /api/v1/admin/users - Lister tous les utilisateurs")
        void getAllUsers_returnsList() throws Exception {
            UserDetailResponse user = UserDetailResponse.builder().email("test@test.com").build();
            when(adminService.getAllUsers()).thenReturn(ApiResponse.success("Success", List.of(user)));

            mockMvc.perform(get("/api/v1/admin/users").with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].email").value("test@test.com"));
        }

        @Test
        @DisplayName("DELETE /api/v1/admin/users/{id} - Supprimer un utilisateur")
        void deleteUser_callsService() throws Exception {
            when(adminService.deleteUser(randomId)).thenReturn(ApiResponse.success("User deleted"));

            mockMvc.perform(delete("/api/v1/admin/users/" + randomId).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User deleted"));
        }
    }

    @Nested
    @DisplayName("Gestion des Cours")
    class CourseManagement {
        @Test
        @DisplayName("GET /api/v1/admin/courses - Lister tous les cours")
        void getAllCourses_returnsList() throws Exception {
            CourseResponse course = new CourseResponse();
            course.setId(1);
            course.setTitle("Java");
            when(adminService.getAllCourses()).thenReturn(ApiResponse.success("Success", List.of(course)));

            mockMvc.perform(get("/api/v1/admin/courses").with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].title").value("Java"));
        }
    }
}
