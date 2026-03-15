package com.ihm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihm.backend.dto.CourseCommentDTO;
import com.ihm.backend.dto.request.CourseCommentRequest;
import com.ihm.backend.dto.response.CourseLikeResponse;
import com.ihm.backend.dto.response.CourseViewResponse;
import com.ihm.backend.entity.User;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.service.CourseInteractionService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour CourseInteractionController.
 *
 * Stratégie : standaloneSetup MockMvc + RequestPostProcessor.
 *
 * Endpoints testés (7) :
 *   POST   /api/courses/{id}/interactions/like             toggleLike
 *   GET    /api/courses/{id}/interactions/like             getLikeStatus
 *   POST   /api/courses/{id}/interactions/view             recordView
 *   GET    /api/courses/{id}/interactions/comments         getComments (public)
 *   POST   /api/courses/{id}/interactions/comments         addComment
 *   PUT    /api/courses/{id}/interactions/comments/{cid}   updateComment
 *   DELETE /api/courses/{id}/interactions/comments/{cid}   deleteComment
 */
@ExtendWith(MockitoExtension.class)
class CourseInteractionControllerTest {

    @Mock
    private CourseInteractionService interactionService;

    @InjectMocks
    private CourseInteractionController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private User studentUser;
    private User teacherUser;

    private CourseLikeResponse likedResponse;
    private CourseLikeResponse unlikedResponse;
    private CourseViewResponse viewRecordedResponse;
    private CourseViewResponse viewAlreadyResponse;
    private CourseCommentDTO sampleComment;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        studentUser = User.builder()
                .id(UUID.randomUUID())
                .email("student@test.com")
                .password("hashed")
                .role(UserRole.STUDENT)
                .firstName("Jean")
                .lastName("Dupont")
                .build();

        teacherUser = User.builder()
                .id(UUID.randomUUID())
                .email("teacher@test.com")
                .password("hashed")
                .role(UserRole.TEACHER)
                .firstName("Marie")
                .lastName("Curie")
                .build();

        likedResponse = CourseLikeResponse.builder().courseId(5).liked(true).likeCount(10).build();
        unlikedResponse = CourseLikeResponse.builder().courseId(5).liked(false).likeCount(9).build();
        viewRecordedResponse = CourseViewResponse.builder().courseId(5).recorded(true).viewCount(50).build();
        viewAlreadyResponse = CourseViewResponse.builder().courseId(5).recorded(false).viewCount(50).build();

        sampleComment = CourseCommentDTO.builder()
                .id(100L)
                .courseId(5)
                .userId(studentUser.getId().toString())
                .userFullName("Jean Dupont")
                .content("Super cours !")
                .createdAt(LocalDateTime.now())
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

    private RequestPostProcessor asTeacher() {
        return (MockHttpServletRequest request) -> {
            request.setUserPrincipal(
                    new UsernamePasswordAuthenticationToken(teacherUser, null,
                            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
            return request;
        };
    }

    // =========================================================================
    // POST /api/courses/{id}/interactions/like — toggleLike
    // =========================================================================
    @Nested
    @DisplayName("POST /like — Toggle like/unlike")
    class ToggleLike {

        @Test
        @DisplayName("✅ STUDENT like un cours → 200 OK, liked=true, likeCount=10")
        void student_likeCourse_returnsLiked() throws Exception {
            when(interactionService.toggleLike(5, studentUser.getId())).thenReturn(likedResponse);

            mockMvc.perform(post("/api/courses/5/interactions/like").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.liked").value(true))
                    .andExpect(jsonPath("$.data.likeCount").value(10))
                    .andExpect(jsonPath("$.message").value("Cours liké avec succès"));

            verify(interactionService).toggleLike(5, studentUser.getId());
        }

        @Test
        @DisplayName("✅ STUDENT unlike → 200 OK, liked=false, likeCount décrémenté")
        void student_unlikeCourse_returnsUnliked() throws Exception {
            when(interactionService.toggleLike(5, studentUser.getId())).thenReturn(unlikedResponse);

            mockMvc.perform(post("/api/courses/5/interactions/like").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.liked").value(false))
                    .andExpect(jsonPath("$.data.likeCount").value(9))
                    .andExpect(jsonPath("$.message").value("Like retiré avec succès"));
        }

        @Test
        @DisplayName("✅ TEACHER peut aussi liker → 200 OK, liked=true")
        void teacher_canLikeCourse() throws Exception {
            CourseLikeResponse teacherLike = CourseLikeResponse.builder()
                    .courseId(5).liked(true).likeCount(11).build();
            when(interactionService.toggleLike(5, teacherUser.getId())).thenReturn(teacherLike);

            mockMvc.perform(post("/api/courses/5/interactions/like").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.liked").value(true));
        }

        @Test
        @DisplayName("✅ Cours introuvable → ResourceNotFoundException → 404")
        void courseNotFound_throws404() throws Exception {
            when(interactionService.toggleLike(99, studentUser.getId()))
                    .thenThrow(new ResourceNotFoundException("Cours non trouvé"));

            mockMvc.perform(post("/api/courses/99/interactions/like").with(asStudent()))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // GET /api/courses/{id}/interactions/like — getLikeStatus
    // =========================================================================
    @Nested
    @DisplayName("GET /like — Statut du like")
    class GetLikeStatus {

        @Test
        @DisplayName("✅ STUDENT ayant liké → liked=true")
        void student_hasLiked_returnsTrue() throws Exception {
            when(interactionService.getLikeStatus(5, studentUser.getId())).thenReturn(likedResponse);

            mockMvc.perform(get("/api/courses/5/interactions/like").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.liked").value(true))
                    .andExpect(jsonPath("$.data.likeCount").value(10));
        }

        @Test
        @DisplayName("✅ STUDENT n'ayant pas liké → liked=false")
        void student_hasNotLiked_returnsFalse() throws Exception {
            when(interactionService.getLikeStatus(5, studentUser.getId())).thenReturn(unlikedResponse);

            mockMvc.perform(get("/api/courses/5/interactions/like").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.liked").value(false));
        }
    }

    // =========================================================================
    // POST /api/courses/{id}/interactions/view — recordView
    // =========================================================================
    @Nested
    @DisplayName("POST /view — Enregistrer une vue")
    class RecordView {

        @Test
        @DisplayName("✅ Première vue STUDENT → recorded=true, viewCount incrémenté")
        void student_firstView_isRecorded() throws Exception {
            when(interactionService.recordView(5, studentUser.getId())).thenReturn(viewRecordedResponse);

            mockMvc.perform(post("/api/courses/5/interactions/view").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.recorded").value(true))
                    .andExpect(jsonPath("$.data.viewCount").value(50))
                    .andExpect(jsonPath("$.message").value("Vue enregistrée"));
        }

        @Test
        @DisplayName("✅ Vue déjà comptabilisée → recorded=false, viewCount stable")
        void student_alreadyViewed_notRecorded() throws Exception {
            when(interactionService.recordView(5, studentUser.getId())).thenReturn(viewAlreadyResponse);

            mockMvc.perform(post("/api/courses/5/interactions/view").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.recorded").value(false))
                    .andExpect(jsonPath("$.message").value("Vue déjà comptabilisée"));
        }

        @Test
        @DisplayName("✅ TEACHER peut enregistrer une vue → recorded=true")
        void teacher_canRecordView() throws Exception {
            CourseViewResponse teacherView = CourseViewResponse.builder()
                    .courseId(5).recorded(true).viewCount(51).build();
            when(interactionService.recordView(5, teacherUser.getId())).thenReturn(teacherView);

            mockMvc.perform(post("/api/courses/5/interactions/view").with(asTeacher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.recorded").value(true));
        }
    }

    // =========================================================================
    // GET /api/courses/{id}/interactions/comments — getComments (public)
    // =========================================================================
    @Nested
    @DisplayName("GET /comments — Lister les commentaires")
    class GetComments {

        @Test
        @DisplayName("✅ Accès sans auth → 200 OK avec liste de commentaires")
        void anonymous_canGetComments() throws Exception {
            when(interactionService.getComments(5)).thenReturn(List.of(sampleComment));

            mockMvc.perform(get("/api/courses/5/interactions/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(100))
                    .andExpect(jsonPath("$.data[0].content").value("Super cours !"))
                    .andExpect(jsonPath("$.data[0].userFullName").value("Jean Dupont"));

            verify(interactionService).getComments(5);
        }

        @Test
        @DisplayName("✅ Aucun commentaire → 200 OK avec liste vide")
        void noComments_returnsEmptyList() throws Exception {
            when(interactionService.getComments(5)).thenReturn(List.of());

            mockMvc.perform(get("/api/courses/5/interactions/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("✅ Cours introuvable → 404")
        void courseNotFound_returns404() throws Exception {
            when(interactionService.getComments(99))
                    .thenThrow(new ResourceNotFoundException("Cours non trouvé"));

            mockMvc.perform(get("/api/courses/99/interactions/comments"))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // POST /api/courses/{id}/interactions/comments — addComment
    // =========================================================================
    @Nested
    @DisplayName("POST /comments — Ajouter un commentaire")
    class AddComment {

        @Test
        @DisplayName("✅ STUDENT ajoute un commentaire → 201 Created")
        void student_canAddComment() throws Exception {
            CourseCommentRequest req = new CourseCommentRequest();
            req.setContent("Super cours !");

            when(interactionService.addComment(5, studentUser.getId(), "Super cours !"))
                    .thenReturn(sampleComment);

            mockMvc.perform(post("/api/courses/5/interactions/comments")
                            .with(asStudent())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").value(100))
                    .andExpect(jsonPath("$.data.content").value("Super cours !"))
                    .andExpect(jsonPath("$.message").value("Commentaire ajouté"));

            verify(interactionService).addComment(5, studentUser.getId(), "Super cours !");
        }

        @Test
        @DisplayName("✅ TEACHER ajoute un commentaire → 201 Created")
        void teacher_canAddComment() throws Exception {
            CourseCommentRequest req = new CourseCommentRequest();
            req.setContent("Excellent contenu pédagogique.");

            CourseCommentDTO teacherComment = CourseCommentDTO.builder()
                    .id(101L).courseId(5)
                    .userId(teacherUser.getId().toString())
                    .userFullName("Marie Curie")
                    .content("Excellent contenu pédagogique.")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(interactionService.addComment(5, teacherUser.getId(), "Excellent contenu pédagogique."))
                    .thenReturn(teacherComment);

            mockMvc.perform(post("/api/courses/5/interactions/comments")
                            .with(asTeacher())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.userFullName").value("Marie Curie"))
                    .andExpect(jsonPath("$.data.content").value("Excellent contenu pédagogique."));
        }
    }

    // =========================================================================
    // PUT /api/courses/{id}/interactions/comments/{commentId} — updateComment
    // =========================================================================
    @Nested
    @DisplayName("PUT /comments/{commentId} — Modifier un commentaire")
    class UpdateComment {

        @Test
        @DisplayName("✅ Auteur modifie son commentaire → 200 OK avec contenu mis à jour")
        void author_canUpdateComment() throws Exception {
            CourseCommentRequest req = new CourseCommentRequest();
            req.setContent("Commentaire modifié");

            CourseCommentDTO updated = CourseCommentDTO.builder()
                    .id(100L).courseId(5)
                    .userId(studentUser.getId().toString())
                    .content("Commentaire modifié")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(interactionService.updateComment(100L, studentUser.getId(), "Commentaire modifié"))
                    .thenReturn(updated);

            mockMvc.perform(put("/api/courses/5/interactions/comments/100")
                            .with(asStudent())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").value("Commentaire modifié"))
                    .andExpect(jsonPath("$.message").value("Commentaire mis à jour"));

            verify(interactionService).updateComment(100L, studentUser.getId(), "Commentaire modifié");
        }

        @Test
        @DisplayName("✅ Non-auteur tente de modifier → AccessDeniedException")
        void nonAuthor_cannotUpdate_serviceThrows() throws Exception {
            CourseCommentRequest req = new CourseCommentRequest();
            req.setContent("Tentative malveillante");

            when(interactionService.updateComment(100L, teacherUser.getId(), "Tentative malveillante"))
                    .thenThrow(new java.nio.file.AccessDeniedException(
                            "Vous ne pouvez modifier que vos propres commentaires"));

            assertThrows(Exception.class, () ->
                    mockMvc.perform(put("/api/courses/5/interactions/comments/100")
                            .with(asTeacher())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))).andReturn());
        }

        @Test
        @DisplayName("✅ Commentaire introuvable → ResourceNotFoundException → 404")
        void commentNotFound_returns404() throws Exception {
            CourseCommentRequest req = new CourseCommentRequest();
            req.setContent("Modification");

            when(interactionService.updateComment(999L, studentUser.getId(), "Modification"))
                    .thenThrow(new ResourceNotFoundException("Commentaire non trouvé"));

            mockMvc.perform(put("/api/courses/5/interactions/comments/999")
                            .with(asStudent())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // DELETE /api/courses/{id}/interactions/comments/{commentId} — deleteComment
    // =========================================================================
    @Nested
    @DisplayName("DELETE /comments/{commentId} — Supprimer un commentaire")
    class DeleteComment {

        @Test
        @DisplayName("✅ Auteur supprime son commentaire → 200 OK")
        void author_canDeleteComment() throws Exception {
            doNothing().when(interactionService).deleteComment(100L, studentUser.getId());

            mockMvc.perform(delete("/api/courses/5/interactions/comments/100").with(asStudent()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Commentaire supprimé"));

            verify(interactionService).deleteComment(100L, studentUser.getId());
        }

        @Test
        @DisplayName("✅ Non-auteur tente de supprimer → AccessDeniedException")
        void nonAuthor_cannotDelete_serviceThrows() throws Exception {
            doThrow(new java.nio.file.AccessDeniedException(
                    "Vous ne pouvez supprimer que vos propres commentaires"))
                    .when(interactionService).deleteComment(100L, teacherUser.getId());

            assertThrows(Exception.class, () ->
                    mockMvc.perform(delete("/api/courses/5/interactions/comments/100")
                            .with(asTeacher())).andReturn());
        }

        @Test
        @DisplayName("✅ Commentaire introuvable → ResourceNotFoundException → 404")
        void commentNotFound_returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Commentaire non trouvé"))
                    .when(interactionService).deleteComment(999L, studentUser.getId());

            mockMvc.perform(delete("/api/courses/5/interactions/comments/999").with(asStudent()))
                    .andExpect(status().isNotFound());
        }
    }
}
