package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.response.StudentResponse;
import com.ihm.backend.dto.response.TeacherResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.GlobalExceptionHandler;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.security.CustomAccessDeniedHandler;
import com.ihm.backend.security.JwtAuthenticationEntryPoint;
import com.ihm.backend.security.JwtAuthenticationFilter;
import com.ihm.backend.service.UserService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    private ObjectMapper objectMapper;

    private User studentUser;
    private final UUID studentId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(studentId)
                .email("student@test.com")
                .role(UserRole.STUDENT)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(studentUser, null, List.of())
        );
    }

    @Nested
    @DisplayName("Récupération de Profil")
    class ProfileRetrieval {
        @Test
        @DisplayName("GET /api/users/students/{id} - Succès")
        void getStudentById_success() throws Exception {
            StudentResponse res = StudentResponse.builder().id(studentId).email("student@test.com").build();
            when(userService.getStudentById(studentId)).thenReturn(res);

            mockMvc.perform(get("/api/users/students/" + studentId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value("student@test.com"));
        }

        @Test
        @DisplayName("GET /api/users/students/{id} - Étudiant non trouvé (404)")
        void getStudentById_notFound() throws Exception {
            when(userService.getStudentById(any()))
                    .thenThrow(new ResourceNotFoundException("Étudiant non trouvé"));

            mockMvc.perform(get("/api/users/students/" + UUID.randomUUID()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("GET /api/users/me - Profil courant (STUDENT)")
        void getCurrentUser_success() throws Exception {
            when(userService.getCurrentUser()).thenReturn(studentUser);

            mockMvc.perform(get("/api/users/me"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Profil étudiant récupéré"))
                    .andExpect(jsonPath("$.data.email").value("student@test.com"));
        }
    }

    @Nested
    @DisplayName("Gestion des Utilisateurs")
    class UserManagement {
        @Test
        @DisplayName("GET /api/users - Liste des utilisateurs (ADMIN)")
        void getAllUsers_success() throws Exception {
            when(userService.getAllUsers()).thenReturn(List.of(studentUser));

            mockMvc.perform(get("/api/users"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].email").value("student@test.com"));
        }

        @Test
        @DisplayName("PUT /api/users/{id} - Mise à jour réussie")
        void updateUser_success() throws Exception {
            User updateReq = User.builder().firstName("NewName").build();
            User updatedUser = User.builder().id(studentId).firstName("NewName").role(UserRole.STUDENT).build();
            
            when(userService.updateUser(eq(studentId), any())).thenReturn(updatedUser);

            mockMvc.perform(put("/api/users/" + studentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Profil étudiant mis à jour"));
        }
    }
}
