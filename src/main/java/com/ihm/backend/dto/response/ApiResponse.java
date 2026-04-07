package com.ihm.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Classe wrapper pour standardiser toutes les réponses API
 * Format unifié: {status, message, data, timestamp}
 *
 * @param <T> Type de données retournées
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Réponse API standardisée")
public class ApiResponse<T> {

    @Schema(description = "Code de statut HTTP", example = "200")
    private int code;

    @Schema(description = "Indique si l'opération a réussi", example = "true")
    private boolean success;

    @Schema(description = "Message décrivant le résultat de l'opération", example = "Opération réussie")
    private String message;

    @Schema(description = "Données de la réponse")
    private T data;

    @Schema(description = "Erreurs de validation (si applicable)")
    private Map<String, String> errors;

    @Schema(description = "Message d'erreur détaillé (si applicable)")
    private String error;

    @Schema(description = "Horodatage de la réponse", example = "2024-01-15T10:30:00")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ==================== MÉTHODES STATIQUES POUR SUCCÈS ====================

    /**
     * Crée une réponse de succès avec données
     *
     * @param message Message de succès
     * @param data    Données à retourner
     * @param <T>     Type de données
     * @return ApiResponse avec statut 200
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse de succès sans données
     *
     * @param message Message de succès
     * @param <T>     Type de données
     * @return ApiResponse avec statut 200
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse de création réussie (201)
     *
     * @param message Message de succès
     * @param data    Données créées
     * @param <T>     Type de données
     * @return ApiResponse avec statut 201
     */
    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .code(201)
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse sans contenu (204)
     *
     * @param message Message de succès
     * @param <T>     Type de données
     * @return ApiResponse avec statut 204
     */
    public static <T> ApiResponse<T> noContent(String message) {
        return ApiResponse.<T>builder()
                .code(204)
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ==================== MÉTHODES STATIQUES POUR ERREURS ====================

    /**
     * Crée une réponse d'erreur générique
     *
     * @param code    Code de statut HTTP
     * @param message Message d'erreur
     * @param error   Détails de l'erreur
     * @param <T>     Type de données
     * @return ApiResponse avec statut d'erreur
     */
    public static <T> ApiResponse<T> error(int code, String message, String error) {
        return ApiResponse.<T>builder()
                .code(code)
                .success(false)
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse d'erreur Bad Request (400)
     *
     * @param message Message d'erreur
     * @param error   Détails de l'erreur
     * @param <T>     Type de données
     * @return ApiResponse avec statut 400
     */
    public static <T> ApiResponse<T> badRequest(String message, String error) {
        return ApiResponse.<T>builder()
                .code(400)
                .success(false)
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse d'erreur de validation (400) avec détails
     *
     * @param message Message d'erreur
     * @param errors  Map des erreurs de validation par champ
     * @param <T>     Type de données
     * @return ApiResponse avec statut 400 et erreurs détaillées
     */
    public static <T> ApiResponse<T> validationError(String message, Map<String, String> errors) {
        return ApiResponse.<T>builder()
                .code(400)
                .success(false)
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse Unauthorized (401)
     *
     * @param message Message d'erreur
     * @param error   Détails de l'erreur
     * @param <T>     Type de données
     * @return ApiResponse avec statut 401
     */
    public static <T> ApiResponse<T> unauthorized(String message, String error) {
        return ApiResponse.<T>builder()
                .code(401)
                .success(false)
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse Forbidden (403)
     *
     * @param message Message d'erreur
     * @param error   Détails de l'erreur
     * @param <T>     Type de données
     * @return ApiResponse avec statut 403
     */
    public static <T> ApiResponse<T> forbidden(String message, String error) {
        return ApiResponse.<T>builder()
                .code(403)
                .success(false)
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse Not Found (404)
     *
     * @param message Message d'erreur
     * @param error   Détails de l'erreur
     * @param <T>     Type de données
     * @return ApiResponse avec statut 404
     */
    public static <T> ApiResponse<T> notFound(String message, String error) {
        return ApiResponse.<T>builder()
                .code(404)
                .success(false)
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse Conflict (409)
     *
     * @param message Message d'erreur
     * @param error   Détails de l'erreur
     * @param <T>     Type de données
     * @return ApiResponse avec statut 409
     */
    public static <T> ApiResponse<T> conflict(String message, String error) {
        return ApiResponse.<T>builder()
                .code(409)
                .success(false)
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse Internal Server Error (500)
     *
     * @param message Message d'erreur
     * @param error   Détails de l'erreur
     * @param <T>     Type de données
     * @return ApiResponse avec statut 500
     */
    public static <T> ApiResponse<T> internalError(String message, String error) {
        return ApiResponse.<T>builder()
                .code(500)
                .success(false)
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Vérifie si la réponse indique un succès
     *
     * @return true si le statut est 2xx
     */
    public boolean isSuccess() {
        return code >= 200 && code < 300;
    }

    /**
     * Vérifie si la réponse indique une erreur
     *
     * @return true si le statut est >= 400
     */
    public boolean isError() {
        return code >= 400;
    }
}


/**
 * Classe pour les réponses paginées
 *
 * @param <T> Type de contenu paginé
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Réponse paginée")
class PageResponse<T> {

    @Schema(description = "Contenu de la page")
    private java.util.List<T> content;

    @Schema(description = "Numéro de la page actuelle", example = "0")
    private int page;

    @Schema(description = "Taille de la page", example = "20")
    private int size;

    @Schema(description = "Nombre total d'éléments", example = "150")
    private long totalElements;

    @Schema(description = "Nombre total de pages", example = "8")
    private int totalPages;

    @Schema(description = "Indique s'il y a une page suivante")
    private boolean hasNext;

    @Schema(description = "Indique s'il y a une page précédente")
    private boolean hasPrevious;

    @Schema(description = "Indique si c'est la première page")
    private boolean first;

    @Schema(description = "Indique si c'est la dernière page")
    private boolean last;

    /**
     * Crée une PageResponse depuis une Page Spring Data
     *
     * @param page Page Spring Data
     * @param <T>  Type de contenu
     * @return PageResponse
     */
    public static <T> PageResponse<T> from(org.springframework.data.domain.Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}