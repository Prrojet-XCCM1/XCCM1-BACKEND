package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.StudentResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private User studentUser;
    private UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();

        studentUser = User.builder()
                .id(studentId)
                .email("student@test.com")
                .role(UserRole.STUDENT)
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

    @Test
    @DisplayName("GET /api/users/students/{id} - Récupérer un étudiant")
    void getStudentById_returnsStudent() throws Exception {
        StudentResponse res = StudentResponse.builder().id(studentId).email("student@test.com").build();
        when(userService.getStudentById(studentId)).thenReturn(res);

        mockMvc.perform(get("/api/users/students/" + studentId).with(asStudent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("student@test.com"));
    }

    @Test
    @DisplayName("GET /api/users/me - Récupérer l'utilisateur courant (STUDENT)")
    void getCurrentUser_returnsProfile() throws Exception {
        when(userService.getCurrentUser()).thenReturn(studentUser);

        mockMvc.perform(get("/api/users/me").with(asStudent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profil étudiant récupéré"))
                .andExpect(jsonPath("$.data.email").value("student@test.com"));
    }

    @Test
    @DisplayName("GET /api/users - Lister tous les utilisateurs (ADMIN)")
    void getAllUsers_returnsList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(studentUser));

        mockMvc.perform(get("/api/users").with(asStudent())) // Roles checked by Spring Security, not by controller in standalone
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].email").value("student@test.com"));
    }
}
