package com.ihm.backend.service;

import com.ihm.backend.dto.CourseCommentDTO;
import com.ihm.backend.dto.response.CourseLikeResponse;
import com.ihm.backend.dto.response.CourseViewResponse;
import com.ihm.backend.entity.*;
import com.ihm.backend.enums.CourseStatus;
import com.ihm.backend.enums.UserRole;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour CourseInteractionService.
 * Couvre : toggle like, recording vue, CRUD commentaires.
 */
@ExtendWith(MockitoExtension.class)
class CourseInteractionServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private CourseLikeRepository courseLikeRepository;
    @Mock private CourseViewRepository courseViewRepository;
    @Mock private CourseCommentRepository courseCommentRepository;

    @InjectMocks
    private CourseInteractionService service;

    private UUID studentId;
    private UUID teacherId;
    private Course publishedCourse;
    private User studentUser;
    private User teacherUser;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        teacherId = UUID.randomUUID();

        studentUser = User.builder().id(studentId).email("s@test.com")
                .role(UserRole.STUDENT).firstName("Jean").lastName("Dupont").build();

        teacherUser = User.builder().id(teacherId).email("t@test.com")
                .role(UserRole.TEACHER).firstName("Marie").lastName("Curie").build();

        publishedCourse = Course.builder()
                .id(5)
                .title("Java Avancé")
                .status(CourseStatus.PUBLISHED)
                .author(teacherUser)
                .likeCount(5L)
                .viewCount(20L)
                .build();
    }

    // =========================================================================
    // LIKES — toggleLike
    // =========================================================================
    @Nested
    @DisplayName("toggleLike — Like / Unlike")
    class ToggleLike {

        @Test
        @DisplayName("✅ Premier like → like enregistré + likeCount incrémenté")
        void firstLike_savesLikeAndIncrements() throws Exception {
            when(courseRepository.findById(5)).thenReturn(Optional.of(publishedCourse));
            when(userRepository.findById(studentId)).thenReturn(Optional.of(studentUser));
            when(courseLikeRepository.existsByCourse_IdAndUser_Id(5, studentId)).thenReturn(false);
            when(courseLikeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(courseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CourseLikeResponse result = service.toggleLike(5, studentId);

            assertThat(result.isLiked()).isTrue();
            assertThat(result.getLikeCount()).isEqualTo(6L);
            verify(courseLikeRepository).save(any(CourseLike.class));
        }

        @Test
        @DisplayName("✅ Like existant → like retiré + likeCount décrémenté")
        void unlikeExisting_deletesLikeAndDecrements() throws Exception {
            when(courseRepository.findById(5)).thenReturn(Optional.of(publishedCourse));
            when(userRepository.findById(studentId)).thenReturn(Optional.of(studentUser));
            when(courseLikeRepository.existsByCourse_IdAndUser_Id(5, studentId)).thenReturn(true);
            when(courseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CourseLikeResponse result = service.toggleLike(5, studentId);

            assertThat(result.isLiked()).isFalse();
            assertThat(result.getLikeCount()).isEqualTo(4L);
            verify(courseLikeRepository).deleteByCourse_IdAndUser_Id(5, studentId);
        }

        @Test
        @DisplayName("✅ Unlike quand likeCount déjà à 0 → likeCount reste à 0 (pas de négatif)")
        void unlike_whenCountIsZero_remainsZero() throws Exception {
            publishedCourse.setLikeCount(0L);
            when(courseRepository.findById(5)).thenReturn(Optional.of(publishedCourse));
            when(userRepository.findById(studentId)).thenReturn(Optional.of(studentUser));
            when(courseLikeRepository.existsByCourse_IdAndUser_Id(5, studentId)).thenReturn(true);
            when(courseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CourseLikeResponse result = service.toggleLike(5, studentId);

            assertThat(result.getLikeCount()).isEqualTo(0L); // Math.max(0, -1) = 0
        }

        @Test
        @DisplayName("✅ Cours introuvable → ResourceNotFoundException")
        void courseNotFound_throwsResourceNotFound() {
            when(courseRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.toggleLike(99, studentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Cours non trouvé");
        }

        @Test
        @DisplayName("✅ Utilisateur introuvable → ResourceNotFoundException")
        void userNotFound_throwsResourceNotFound() {
            when(courseRepository.findById(5)).thenReturn(Optional.of(publishedCourse));
            when(userRepository.findById(studentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.toggleLike(5, studentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // LIKES — getLikeStatus
    // =========================================================================
    @Nested
    @DisplayName("getLikeStatus — Statut du like")
    class GetLikeStatus {

        @Test
        @DisplayName("✅ Utilisateur ayant liké → liked=true avec bon likeCount")
        void userHasLiked_returnsTrue() {
            when(courseRepository.findById(5)).thenReturn(Optional.of(publishedCourse));
            when(courseLikeRepository.existsByCourse_IdAndUser_Id(5, studentId)).thenReturn(true);

            CourseLikeResponse result = service.getLikeStatus(5, studentId);

            assertThat(result.isLiked()).isTrue();
            assertThat(result.getLikeCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("✅ Utilisateur n'ayant pas liké → liked=false")
        void userHasNotLiked_returnsFalse() {
            when(courseRepository.findById(5)).thenReturn(Optional.of(publishedCourse));
            when(courseLikeRepository.existsByCourse_IdAndUser_Id(5, studentId)).thenReturn(false);

            CourseLikeResponse result = service.getLikeStatus(5, studentId);

            assertThat(result.isLiked()).isFalse();
        }
    }

    // =========================================================================
    // VUES — recordView
    // =========================================================================
    @Nested
    @DisplayName("recordView — Enregistrement de vue")
    class RecordView {

        @Test
        @DisplayName("✅ Première vue → enregistrée, viewCount incrémenté, recorded=true")
        void firstView_isRecordedAndCountIncremented() throws Exception {
            when(courseRepository.findById(5)).thenReturn(Optional.of(publishedCourse));
            when(userRepository.findById(studentId)).thenReturn(Optional.of(studentUser));
            when(courseViewRepository.existsByCourse_IdAndUser_Id(5, studentId)).thenReturn(false);
            when(courseViewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(courseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CourseViewResponse result = service.recordView(5, studentId);

            assertThat(result.isRecorded()).isTrue();
            assertThat(result.getViewCount()).isEqualTo(21L);

            ArgumentCaptor<CourseView> captor = ArgumentCaptor.forClass(CourseView.class);
            verify(courseViewRepository).save(captor.capture());
            assertThat(captor.getValue().getUser().getId()).isEqualTo(studentId);
        }

        @Test
        @DisplayName("✅ Vue déjà comptabilisée → recorded=false, viewCount inchangé")
        void alreadyViewed_notRecordedAgain() throws Exception {
            when(courseRepository.findById(5)).thenReturn(Optional.of(publishedCourse));
            when(userRepository.findById(studentId)).thenReturn(Optional.of(studentUser));
            when(courseViewRepository.existsByCourse_IdAndUser_Id(5, studentId)).thenReturn(true);

            CourseViewResponse result = service.recordView(5, studentId);

            assertThat(result.isRecorded()).isFalse();
            assertThat(result.getViewCount()).isEqualTo(20L);
            verify(courseViewRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ Cours introuvable → ResourceNotFoundException")
        void courseNotFound_throwsException() {
            when(courseRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.recordView(99, studentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // COMMENTAIRES — addComment
    // =========================================================================
    @Nested
    @DisplayName("addComment — Ajouter un commentaire")
    class AddComment {

        @Test
        @DisplayName("✅ STUDENT ajoute un commentaire → DTO retourné avec bon contenu")
        void student_addComment_returnsDTO() throws Exception {
            when(courseRepository.findById(5)).thenReturn(Optional.of(publishedCourse));
            when(userRepository.findById(studentId)).thenReturn(Optional.of(studentUser));

            CourseComment saved = CourseComment.builder()
                    .id(1L).course(publishedCourse).user(studentUser)
                    .content("Excellent cours !").build();
            when(courseCommentRepository.save(any())).thenReturn(saved);

            CourseCommentDTO result = service.addComment(5, studentId, "  Excellent cours !  ");

            assertThat(result.getContent()).isEqualTo("Excellent cours !");
            assertThat(result.getUserFullName()).isEqualTo("Jean Dupont");
            assertThat(result.getCourseId()).isEqualTo(5);

            ArgumentCaptor<CourseComment> captor = ArgumentCaptor.forClass(CourseComment.class);
            verify(courseCommentRepository).save(captor.capture());
            assertThat(captor.getValue().getContent()).isEqualTo("Excellent cours !"); // trimmed
        }

        @Test
        @DisplayName("✅ TEACHER peut aussi commenter → DTO retourné")
        void teacher_canAddComment() throws Exception {
            when(courseRepository.findById(5)).thenReturn(Optional.of(publishedCourse));
            when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacherUser));

            CourseComment saved = CourseComment.builder()
                    .id(2L).course(publishedCourse).user(teacherUser)
                    .content("Bravo pour ce cours.").build();
            when(courseCommentRepository.save(any())).thenReturn(saved);

            CourseCommentDTO result = service.addComment(5, teacherId, "Bravo pour ce cours.");

            assertThat(result.getUserFullName()).isEqualTo("Marie Curie");
        }
    }

    // =========================================================================
    // COMMENTAIRES — updateComment
    // =========================================================================
    @Nested
    @DisplayName("updateComment — Modifier un commentaire")
    class UpdateComment {

        @Test
        @DisplayName("✅ Auteur modifie son propre commentaire → DTO mis à jour")
        void author_canUpdateOwnComment() throws Exception {
            CourseComment existing = CourseComment.builder()
                    .id(10L).course(publishedCourse).user(studentUser)
                    .content("Ancien contenu").build();

            CourseComment updated = CourseComment.builder()
                    .id(10L).course(publishedCourse).user(studentUser)
                    .content("Nouveau contenu").build();

            when(courseCommentRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(courseCommentRepository.save(any())).thenReturn(updated);

            CourseCommentDTO result = service.updateComment(10L, studentId, "  Nouveau contenu  ");

            assertThat(result.getContent()).isEqualTo("Nouveau contenu");
            verify(courseCommentRepository).save(existing);
        }

        @Test
        @DisplayName("✅ Non-auteur tente de modifier → AccessDeniedException")
        void nonAuthor_cannotUpdate_throwsAccessDenied() {
            CourseComment existing = CourseComment.builder()
                    .id(10L).course(publishedCourse).user(studentUser)
                    .content("Commentaire").build();

            when(courseCommentRepository.findById(10L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.updateComment(10L, teacherId, "Hack"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("propres commentaires");
        }

        @Test
        @DisplayName("✅ Commentaire introuvable → ResourceNotFoundException")
        void commentNotFound_throwsException() {
            when(courseCommentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateComment(999L, studentId, "X"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // COMMENTAIRES — deleteComment
    // =========================================================================
    @Nested
    @DisplayName("deleteComment — Supprimer un commentaire")
    class DeleteComment {

        @Test
        @DisplayName("✅ Auteur supprime son commentaire → delete appelé")
        void author_canDeleteOwnComment() throws Exception {
            CourseComment existing = CourseComment.builder()
                    .id(10L).course(publishedCourse).user(studentUser)
                    .content("À supprimer").build();

            when(courseCommentRepository.findById(10L)).thenReturn(Optional.of(existing));

            service.deleteComment(10L, studentId);

            verify(courseCommentRepository).delete(existing);
        }

        @Test
        @DisplayName("✅ Non-auteur tente de supprimer → AccessDeniedException")
        void nonAuthor_cannotDelete_throwsAccessDenied() {
            CourseComment existing = CourseComment.builder()
                    .id(10L).course(publishedCourse).user(studentUser)
                    .content("Commentaire").build();

            when(courseCommentRepository.findById(10L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.deleteComment(10L, teacherId))
                    .isInstanceOf(AccessDeniedException.class);

            verify(courseCommentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("✅ Commentaire introuvable → ResourceNotFoundException")
        void commentNotFound_throwsException() {
            when(courseCommentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteComment(999L, studentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // COMMENTAIRES — getComments
    // =========================================================================
    @Nested
    @DisplayName("getComments — Lister les commentaires")
    class GetComments {

        @Test
        @DisplayName("✅ Cours avec commentaires → liste ordonnée retournée")
        void courseWithComments_returnsList() {
            when(courseRepository.existsById(5)).thenReturn(true);

            CourseComment c1 = CourseComment.builder().id(1L).course(publishedCourse)
                    .user(studentUser).content("Premier").build();
            CourseComment c2 = CourseComment.builder().id(2L).course(publishedCourse)
                    .user(teacherUser).content("Deuxième").build();

            when(courseCommentRepository.findByCourse_IdOrderByCreatedAtDesc(5))
                    .thenReturn(List.of(c2, c1)); // plus récent en premier

            List<CourseCommentDTO> result = service.getComments(5);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getContent()).isEqualTo("Deuxième");
        }

        @Test
        @DisplayName("✅ Aucun commentaire → liste vide")
        void noComments_returnsEmptyList() {
            when(courseRepository.existsById(5)).thenReturn(true);
            when(courseCommentRepository.findByCourse_IdOrderByCreatedAtDesc(5))
                    .thenReturn(List.of());

            List<CourseCommentDTO> result = service.getComments(5);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("✅ Cours introuvable → ResourceNotFoundException")
        void courseNotFound_throwsException() {
            when(courseRepository.existsById(99)).thenReturn(false);

            assertThatThrownBy(() -> service.getComments(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Cours non trouvé");
        }
    }
}
