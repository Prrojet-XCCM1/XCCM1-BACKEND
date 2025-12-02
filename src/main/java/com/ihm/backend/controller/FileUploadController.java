package cm.enspy.xccm.controller;

import cm.enspy.xccm.domain.dto.response.ApiResponse;
import cm.enspy.xccm.security.SecurityUtils;
import cm.enspy.xccm.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
// Imports Réactifs
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart; // Pour l'upload de fichiers réactif
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange; // Pour l'accès aux infos de la requête réactive
import reactor.core.publisher.Mono; // Type de retour fondamental en WebFlux
import reactor.core.publisher.Flux; // Pour les collections réactives (fichiers multiples)

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Contrôleur pour la gestion des uploads de fichiers (Version WebFlux/Réactive).
 * Gère les photos de profil et les fichiers de composition
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fichiers", description = "Gestion des uploads de fichiers")
@SecurityRequirement(name = "Bearer Authentication")
public class FileUploadController {

    private final FileStorageService fileStorageService;
    
    // --- Méthode utilitaire simple pour déterminer le type de contenu (nécessaire car HttpServletRequest n'existe plus) ---
    private String determineContentType(String fileName) {
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
        if (fileName.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (fileName.endsWith(".gif")) return MediaType.IMAGE_GIF_VALUE;
        if (fileName.endsWith(".pdf")) return MediaType.APPLICATION_PDF_VALUE;
        // Ajoutez d'autres types si nécessaire
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }


    /**
     * Upload d'une photo de profil (Réactif)
     */
    @Operation(
        summary = "Upload photo de profil",
        description = "Téléverse une photo de profil pour l'utilisateur connecté (max 5MB, formats: JPG, PNG, GIF, WEBP)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Photo uploadée avec succès"),
         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Fichier invalide (taille, format)"),
         @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    // Retourne Mono<ResponseEntity> et prend Mono<FilePart>
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> uploadProfilePicture(
            @Parameter(description = "Fichier image à uploader", required = true)
            @RequestPart("file") Mono<FilePart> filePartMono
    ) {
        // 1. Récupérer l'ID utilisateur de manière réactive
        Mono<UUID> userIdMono = SecurityUtils.getCurrentUserIdReactive()
                .switchIfEmpty(Mono.error(() -> new RuntimeException("Utilisateur non authentifié (Réactif)")));

        // 2. Combiner l'ID utilisateur et le fichier, puis traiter l'upload
        return Mono.zip(userIdMono, filePartMono)
                .flatMap(tuple -> {
                    UUID userId = tuple.getT1();
                    FilePart filePart = tuple.getT2();

                    log.info("Upload de photo de profil (WebFlux) - Nom: {}", filePart.filename());

                    // ASSUMPTION: fileStorageService.storeProfilePicture() est maintenant réactif et retourne Mono<String> (le nom de fichier)
                    return fileStorageService.storeProfilePicture(filePart, userId)
                            .map(fileName -> {
                                String fileUrl = "/api/v1/files/profile-picture/" + fileName;
                                
                                Map<String, String> response = new HashMap<>();
                                response.put("fileName", fileName);
                                response.put("fileUrl", fileUrl);
                                response.put("fileType", filePart.headers().getContentType().toString());
                                // La taille doit être gérée dans le service car elle nécessite de consommer le Flux
                                response.put("fileSize", "Taille déterminée côté service"); 

                                return ResponseEntity.ok(
                                        ApiResponse.success("Photo de profil uploadée avec succès", response)
                                );
                            });
                });
    }

    /**
     * Téléchargement d'une photo de profil (Réactif)
     */
    @Operation(
        summary = "Télécharger photo de profil",
        description = "Récupère une photo de profil par son nom de fichier"
    )
    @GetMapping("/profile-picture/{fileName:.+}")
    // Retourne Mono<ResponseEntity<Resource>> et utilise ServerWebExchange au lieu de HttpServletRequest
    public Mono<ResponseEntity<Resource>> downloadProfilePicture(
            @Parameter(description = "Nom du fichier", required = true)
            @PathVariable String fileName,
            ServerWebExchange exchange
    ) {
        log.debug("Téléchargement de photo de profil (WebFlux): {}", fileName);

        // ASSUMPTION: fileStorageService.loadFileAsResource() retourne Mono<Resource>
        return fileStorageService.loadFileAsResource(fileName, "profile")
                .map(resource -> {
                    String contentType = determineContentType(fileName);
                    
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                            .body(resource);
                })
                // Gérer le cas où la ressource n'est pas trouvée (Mono est vide)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Suppression d'une photo de profil (Réactif)
     */
    @Operation(
        summary = "Supprimer photo de profil",
        description = "Supprime la photo de profil de l'utilisateur connecté"
    )
    @DeleteMapping("/profile-picture/{fileName}")
    @PreAuthorize("isAuthenticated()")
    // Retourne Mono<ResponseEntity<ApiResponse<Void>>>
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteProfilePicture(
            @PathVariable String fileName
    ) {
        log.info("Suppression de photo de profil (WebFlux): {}", fileName);

        // ASSUMPTION: fileStorageService.deleteFile() retourne Mono<Boolean>
        return fileStorageService.deleteFile(fileName, "profile")
                .map(deleted -> {
                    if (deleted) {
                        return ResponseEntity.ok(
                                ApiResponse.success("Photo de profil supprimée avec succès")
                        );
                    } else {
                        return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.notFound("Photo de profil introuvable", fileName));
                    }
                });
    }

    /**
     * Upload d'un fichier de composition (Réactif)
     */
    @Operation(
        summary = "Upload fichier composition",
        description = "Téléverse un fichier pour une composition (max 20MB, formats: PDF, DOC, DOCX)"
    )
    @PostMapping(value = "/composition/{compositionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    // Prend Mono<FilePart>
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> uploadCompositionFile(
            @Parameter(description = "ID de la composition", required = true)
            @PathVariable UUID compositionId,
            
            @Parameter(description = "Fichier à uploader", required = true)
            @RequestPart("file") Mono<FilePart> filePartMono
    ) {
        return filePartMono
                .flatMap(filePart -> {
                    log.info("Upload de fichier pour composition: {} - Nom: {}", compositionId, filePart.filename());

                    // ASSUMPTION: fileStorageService.storeCompositionFile() est réactif
                    return fileStorageService.storeCompositionFile(filePart, compositionId)
                            .map(fileName -> {
                                String fileUrl = "/api/v1/files/composition/" + compositionId + "/" + fileName;

                                Map<String, String> response = new HashMap<>();
                                response.put("fileName", fileName);
                                response.put("fileUrl", fileUrl);
                                response.put("fileType", filePart.headers().getContentType().toString());
                                response.put("fileSize", "Taille déterminée côté service");

                                return ResponseEntity.ok(
                                        ApiResponse.success("Fichier uploadé avec succès", response)
                                );
                            });
                });
    }

    /**
     * Téléchargement d'un fichier de composition (Réactif)
     */
    @Operation(
        summary = "Télécharger fichier composition",
        description = "Récupère un fichier de composition"
    )
    @GetMapping("/composition/{compositionId}/{fileName:.+}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
    // Retourne Mono<ResponseEntity<Resource>>
    public Mono<ResponseEntity<Resource>> downloadCompositionFile(
            @PathVariable UUID compositionId,
            @PathVariable String fileName,
            ServerWebExchange exchange // Remplacement de HttpServletRequest
    ) {
        log.debug("Téléchargement de fichier de composition (WebFlux): {}", fileName);

        // ASSUMPTION: fileStorageService.loadFileAsResource() retourne Mono<Resource>
        return fileStorageService.loadFileAsResource(fileName, "composition")
                .map(resource -> {
                    String contentType = determineContentType(fileName);
                    
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            // Utiliser "attachment" pour forcer le téléchargement
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                            .body(resource);
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Upload multiple de fichiers (Réactif)
     */
    @Operation(
        summary = "Upload multiple de fichiers",
        description = "Téléverse plusieurs fichiers simultanément"
    )
    @PostMapping(value = "/composition/{compositionId}/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    // Prend Flux<FilePart>
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> uploadMultipleFiles(
            @PathVariable UUID compositionId,
            @RequestPart("files") Flux<FilePart> filesFlux
    ) {
        // Pour gérer les multiples uploads, nous traitons le Flux de FilePart et accumulons les résultats.
        return filesFlux.flatMap(filePart -> 
                fileStorageService.storeCompositionFile(filePart, compositionId)
                    .thenReturn(true) // Succès: retourne un booléen pour compter
                    .onErrorResume(ex -> { // Échec: log l'erreur et retourne false
                        log.error("Erreur lors de l'upload de: {}", filePart.filename(), ex);
                        return Mono.just(false);
                    })
            )
            .collectList() // Rassembler tous les résultats de succès/échec
            .map(results -> {
                long successCount = results.stream().filter(r -> r).count();
                long totalFiles = results.size();
                long failCount = totalFiles - successCount;
                
                log.info("Upload de {} fichiers pour la composition: {} ({} réussis, {} échoués)", 
                        totalFiles, compositionId, successCount, failCount);

                Map<String, Object> response = new HashMap<>();
                response.put("total", totalFiles);
                response.put("success", successCount);
                response.put("failed", failCount);

                return ResponseEntity.ok(
                        ApiResponse.success(
                                String.format("%d fichiers uploadés avec succès sur %d", successCount, totalFiles),
                                response
                        )
                );
            });
    }
}

