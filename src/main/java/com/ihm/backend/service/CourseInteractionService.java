package com.ihm.backend.service;

import com.ihm.backend.dto.CourseCommentDTO;
import com.ihm.backend.dto.response.CourseLikeResponse;
import com.ihm.backend.dto.response.CourseViewResponse;
import com.ihm.backend.entity.*;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.repository.jpa.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseInteractionService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseLikeRepository courseLikeRepository;
    private final CourseViewRepository courseViewRepository;
    private final CourseCommentRepository courseCommentRepository;

    // ============================================================
    //  LIKES
    // ============================================================

    /**
     * Toggle like/unlike sur un cours.
     * Si l'utilisateur n'a pas encore liké → like ajouté + likeCount incrémenté.
     * Si l'utilisateur a déjà liké → like retiré + likeCount décrémenté.
     */
    @Transactional
    public CourseLikeResponse toggleLike(Integer courseId, UUID userId) throws Exception {
        Course course = getCourse(courseId);
        User user = getUser(userId);

        boolean alreadyLiked = courseLikeRepository.existsByCourse_IdAndUser_Id(courseId, userId);

        if (alreadyLiked) {
            courseLikeRepository.deleteByCourse_IdAndUser_Id(courseId, userId);
            long newCount = Math.max(0, course.getLikeCount() - 1);
            course.setLikeCount(newCount);
            courseRepository.save(course);
            log.info("Like retiré: userId={}, courseId={}, likeCount={}", userId, courseId, newCount);
            return CourseLikeResponse.builder()
                    .courseId(courseId)
                    .liked(false)
                    .likeCount(newCount)
                    .build();
        } else {
            CourseLike like = CourseLike.builder().course(course).user(user).build();
            courseLikeRepository.save(like);
            long newCount = course.getLikeCount() + 1;
            course.setLikeCount(newCount);
            courseRepository.save(course);
            log.info("Like ajouté: userId={}, courseId={}, likeCount={}", userId, courseId, newCount);
            return CourseLikeResponse.builder()
                    .courseId(courseId)
                    .liked(true)
                    .likeCount(newCount)
                    .build();
        }
    }

    /**
     * Retourne le statut du like de l'utilisateur pour un cours.
     */
    public CourseLikeResponse getLikeStatus(Integer courseId, UUID userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));
        boolean liked = courseLikeRepository.existsByCourse_IdAndUser_Id(courseId, userId);
        return CourseLikeResponse.builder()
                .courseId(courseId)
                .liked(liked)
                .likeCount(course.getLikeCount())
                .build();
    }

    // ============================================================
    //  VUES
    // ============================================================

    /**
     * Enregistre la vue d'un utilisateur si elle n'existe pas encore.
     * Met à jour le compteur de vues du cours.
     */
    @Transactional
    public CourseViewResponse recordView(Integer courseId, UUID userId) throws Exception {
        Course course = getCourse(courseId);
        User user = getUser(userId);

        boolean alreadyViewed = courseViewRepository.existsByCourse_IdAndUser_Id(courseId, userId);
        if (alreadyViewed) {
            log.debug("Vue déjà enregistrée: userId={}, courseId={}", userId, courseId);
            return CourseViewResponse.builder()
                    .courseId(courseId)
                    .recorded(false)
                    .viewCount(course.getViewCount())
                    .build();
        }

        CourseView view = CourseView.builder().course(course).user(user).build();
        courseViewRepository.save(view);
        long newCount = course.getViewCount() + 1;
        course.setViewCount(newCount);
        courseRepository.save(course);
        log.info("Vue enregistrée: userId={}, courseId={}, viewCount={}", userId, courseId, newCount);

        return CourseViewResponse.builder()
                .courseId(courseId)
                .recorded(true)
                .viewCount(newCount)
                .build();
    }

    // ============================================================
    //  COMMENTAIRES
    // ============================================================

    /**
     * Ajoute un commentaire sur un cours.
     */
    @Transactional
    public CourseCommentDTO addComment(Integer courseId, UUID userId, String content) throws Exception {
        Course course = getCourse(courseId);
        User user = getUser(userId);

        CourseComment comment = CourseComment.builder()
                .course(course)
                .user(user)
                .content(content.trim())
                .build();

        CourseComment saved = courseCommentRepository.save(comment);
        log.info("Commentaire ajouté: userId={}, courseId={}, commentId={}", userId, courseId, saved.getId());
        return CourseCommentDTO.fromEntity(saved);
    }

    /**
     * Met à jour le contenu d'un commentaire.
     * Seul l'auteur du commentaire peut le modifier.
     */
    @Transactional
    public CourseCommentDTO updateComment(Long commentId, UUID userId, String content) throws Exception {
        CourseComment comment = courseCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire non trouvé"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Vous ne pouvez modifier que vos propres commentaires");
        }

        comment.setContent(content.trim());
        CourseComment saved = courseCommentRepository.save(comment);
        log.info("Commentaire mis à jour: commentId={}, userId={}", commentId, userId);
        return CourseCommentDTO.fromEntity(saved);
    }

    /**
     * Supprime un commentaire.
     * Seul l'auteur du commentaire peut le supprimer.
     */
    @Transactional
    public void deleteComment(Long commentId, UUID userId) throws Exception {
        CourseComment comment = courseCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire non trouvé"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres commentaires");
        }

        courseCommentRepository.delete(comment);
        log.info("Commentaire supprimé: commentId={}, userId={}", commentId, userId);
    }

    /**
     * Récupère tous les commentaires d'un cours, triés du plus récent au plus ancien.
     */
    @Transactional(readOnly = true)
    public List<CourseCommentDTO> getComments(Integer courseId) {
        // Vérifie que le cours existe
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Cours non trouvé");
        }
        return courseCommentRepository.findByCourse_IdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(CourseCommentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Récupère tous les commentaires des étudiants sur un cours, réservé à l'enseignant propriétaire.
     * Vérifie que l'enseignant est bien l'auteur du cours avant de retourner les commentaires.
     */
    @Transactional(readOnly = true)
    public List<CourseCommentDTO> getCourseCommentsForTeacher(Integer courseId, UUID teacherId) throws java.nio.file.AccessDeniedException {
        Course course = getCourse(courseId);

        if (!course.getAuthor().getId().equals(teacherId)) {
            throw new java.nio.file.AccessDeniedException(
                    "Seul l'enseignant auteur du cours peut consulter les commentaires depuis ce point d'accès");
        }

        return courseCommentRepository.findByCourse_IdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(CourseCommentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // ============================================================
    //  Utilitaires privés
    // ============================================================

    private Course getCourse(Integer courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }
}
