package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.CourseCommentDTO;
import com.ihm.backend.dto.request.CourseCommentRequest;
import com.ihm.backend.dto.response.CourseLikeResponse;
import com.ihm.backend.dto.response.CourseViewResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.GlobalExceptionHandler;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.security.CustomAccessDeniedHandler;
import com.ihm.backend.security.JwtAuthenticationEntryPoint;
import com.ihm.backend.security.JwtAuthenticationFilter;
import com.ihm.backend.security.oauth2.CustomOAuth2UserService;
import com.ihm.backend.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.ihm.backend.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.ihm.backend.service.CourseInteractionService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(CourseInteractionController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseInteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseInteractionService interactionService;

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

    private User studentUser;
    private final UUID studentId = UUID.fromString("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA");

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(studentId)
                .email("student@test.com")
                .role(UserRole.STUDENT)
                .build();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }

    @Nested
    @DisplayName("Interactions Likes")
    class Likes {
        @Test
        @DisplayName("POST /api/courses/{id}/interactions/like - Toggle Like (Succès)")
        void toggleLike_success() throws Exception {
            authenticateAs(studentUser);
            CourseLikeResponse res = CourseLikeResponse.builder().liked(true).likeCount(10).build();
            when(interactionService.toggleLike(eq(5), eq(studentId))).thenReturn(res);

            mockMvc.perform(post("/api/courses/5/interactions/like")
                            .with(user(studentUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.liked").value(true));
        }
    }

    @Nested
    @DisplayName("Interactions Vues")
    class Views {
        @Test
        @DisplayName("POST /api/courses/{id}/interactions/view - Enregistrer Vue (Succès)")
        void recordView_success() throws Exception {
            authenticateAs(studentUser);
            CourseViewResponse res = CourseViewResponse.builder().recorded(true).viewCount(50).build();
            when(interactionService.recordView(eq(5), eq(studentId))).thenReturn(res);

            mockMvc.perform(post("/api/courses/5/interactions/view")
                            .with(user(studentUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.recorded").value(true));
        }
    }

    @Nested
    @DisplayName("Commentaires")
    class Comments {
        @Test
        @DisplayName("GET /api/courses/{id}/interactions/comments - Lister (Succès)")
        void getComments_success() throws Exception {
            CourseCommentDTO comment = CourseCommentDTO.builder().id(100L).content("Super !").build();
            when(interactionService.getComments(eq(5))).thenReturn(List.of(comment));

            mockMvc.perform(get("/api/courses/5/interactions/comments")
                            .with(user(studentUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].content").value("Super !"));
        }

        @Test
        @DisplayName("POST /api/courses/{id}/interactions/comments - Ajouter (Succès)")
        void addComment_success() throws Exception {
            authenticateAs(studentUser);
            CourseCommentRequest req = new CourseCommentRequest();
            req.setContent("Excellent cours");
            CourseCommentDTO res = CourseCommentDTO.builder().id(101L).content("Excellent cours").build();

            when(interactionService.addComment(eq(5), eq(studentId), eq("Excellent cours"))).thenReturn(res);

            mockMvc.perform(post("/api/courses/5/interactions/comments")
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.content").value("Excellent cours"));
        }

        @Test
        @DisplayName("DELETE /api/courses/{id}/interactions/comments/{cid} - Succès auteur")
        void deleteComment_success() throws Exception {
            authenticateAs(studentUser);
            doNothing().when(interactionService).deleteComment(eq(10L), eq(studentId));

            mockMvc.perform(delete("/api/courses/5/interactions/comments/10")
                            .with(user(studentUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("DELETE /api/courses/{id}/interactions/comments/{cid} - Erreur 404")
        void deleteComment_notFound() throws Exception {
            authenticateAs(studentUser);
            doThrow(new ResourceNotFoundException("Commentaire non trouvé"))
                    .when(interactionService).deleteComment(eq(999L), eq(studentId));

            mockMvc.perform(delete("/api/courses/5/interactions/comments/999")
                            .with(user(studentUser)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("PUT /api/courses/{id}/interactions/comments/{cid} - Mise à jour réussie")
        void updateComment_success() throws Exception {
            authenticateAs(studentUser);
            CourseCommentRequest req = new CourseCommentRequest();
            req.setContent("Contenu mis à jour");
            CourseCommentDTO updated = CourseCommentDTO.builder().id(10L).content("Contenu mis à jour").build();

            when(interactionService.updateComment(eq(10L), eq(studentId), eq("Contenu mis à jour")))
                    .thenReturn(updated);

            mockMvc.perform(put("/api/courses/5/interactions/comments/10")
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").value("Contenu mis à jour"));
        }

        @Test
        @DisplayName("POST /api/courses/{id}/interactions/comments - Contenu vide → 400")
        void addComment_emptyContent_returns400() throws Exception {
            authenticateAs(studentUser);
            CourseCommentRequest req = new CourseCommentRequest();
            req.setContent("   ");

            mockMvc.perform(post("/api/courses/5/interactions/comments")
                            .with(user(studentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/courses/{id}/interactions/like - Statut du like")
        void getLikeStatus_success() throws Exception {
            authenticateAs(studentUser);
            CourseLikeResponse res = CourseLikeResponse.builder().liked(false).likeCount(3).build();
            when(interactionService.getLikeStatus(eq(5), eq(studentId))).thenReturn(res);

            mockMvc.perform(get("/api/courses/5/interactions/like")
                            .with(user(studentUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.liked").value(false))
                    .andExpect(jsonPath("$.data.likeCount").value(3));
        }

        @Test
        @DisplayName("DELETE /api/courses/{id}/interactions/comments/{cid}/admin - Admin supprime un commentaire")
        void adminDeleteComment_success() throws Exception {
            User adminUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("admin@test.com")
                    .role(UserRole.ADMIN)
                    .build();
            doNothing().when(interactionService).deleteCommentAsAdmin(eq(10L));

            mockMvc.perform(delete("/api/courses/5/interactions/comments/10/admin")
                            .with(user(adminUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
