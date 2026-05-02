package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.request.CourseInvitationRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.CourseInvitationResponse;
import com.ihm.backend.entity.Course;
import com.ihm.backend.entity.CourseInvitation;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.InvitationStatus;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.GlobalExceptionHandler;
import com.ihm.backend.mappers.CourseInvitationMapper;
import com.ihm.backend.security.CustomAccessDeniedHandler;
import com.ihm.backend.security.oauth2.CustomOAuth2UserService;
import com.ihm.backend.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.ihm.backend.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.ihm.backend.security.JwtAuthenticationEntryPoint;
import com.ihm.backend.security.JwtAuthenticationFilter;
import com.ihm.backend.service.CourseInvitationService;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(CourseInvitationController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseInvitationService invitationService;

    @MockitoBean
    private CourseInvitationMapper invitationMapper;

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

    private User teacherUser;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder()
                .id(UUID.randomUUID())
                .email("teacher@test.com")
                .role(UserRole.TEACHER)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(teacherUser, null, List.of())
        );
    }

    @Test
    void inviteEditor_success() throws Exception {
        CourseInvitationRequest req = new CourseInvitationRequest();
        req.setCourseId(1);
        req.setEmailOrName("invited@test.com");

        CourseInvitation invitation = CourseInvitation.builder()
                .id(1L)
                .email("invited@test.com")
                .token("test-token")
                .status(InvitationStatus.PENDING)
                .expiryDate(LocalDateTime.now().plusDays(7))
                // course and inviter are null for now to avoid mockito complexity, 
                // but in reality they would be proxies if lazy loaded
                .build();

        when(invitationService.inviteEditor(anyInt(), anyString(), any())).thenReturn(invitation);
        
        CourseInvitationResponse response = CourseInvitationResponse.builder()
                .id(1L)
                .email("invited@test.com")
                .token("test-token")
                .status(InvitationStatus.PENDING)
                .build();
                
        when(invitationMapper.toResponse(any())).thenReturn(response);

        mockMvc.perform(post("/api/invitations/invite")
                        .with(user(teacherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
