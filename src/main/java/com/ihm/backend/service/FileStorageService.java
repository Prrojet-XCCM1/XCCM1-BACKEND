package cm.enspy.xccm.service;

import cm.enspy.xccm.config.FileStorageConfig;
import cm.enspy.xccm.exception.FileProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Service réactif de gestion du stockage de fichiers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileStorageConfig fileStorageConfig;

    /**
     * Stocke une photo de profil (Réactif)
     */
    @CacheEvict(value = "users", key = "#userId")
    public Mono<String> storeProfilePicture(FilePart file, UUID userId) {
        // Validation du fichier
        validateProfilePicture(file);
        
        // Générer un nom unique
        String fileName = generateUniqueFileName(file, "profile_" + userId);
        
        try {
            Path targetLocation = fileStorageConfig.getProfilePicturesPath().resolve(fileName);
            
            // Transfert réactif du fichier
            return file.transferTo(targetLocation)
                .then(Mono.fromCallable(() -> {
                    log.info("Photo de profil stockée: {} pour l'utilisateur: {}", fileName, userId);
                    return fileName;
                }))
                .onErrorMap(ex -> {
                    log.error("Erreur lors du stockage de la photo de profil", ex);
                    return new FileProcessingException("Impossible de stocker le fichier: " + fileName, ex);
                });
            
        } catch (Exception ex) {
            log.error("Erreur lors du stockage de la photo de profil", ex);
            return Mono.error(new FileProcessingException("Impossible de stocker le fichier: " + fileName, ex));
        }
    }

    /**
     * Stocke un fichier de composition (Réactif)
     */
    public Mono<String> storeCompositionFile(FilePart file, UUID compositionId) {
        validateDocument(file);
        String fileName = generateUniqueFileName(file, "comp_" + compositionId);
        
        try {
            Path targetLocation = fileStorageConfig.getCompositionFilesPath().resolve(fileName);
            
            return file.transferTo(targetLocation)
                .then(Mono.fromCallable(() -> {
                    log.info("Fichier de composition stocké: {} pour la composition: {}", fileName, compositionId);
                    return fileName;
                }))
                .onErrorMap(ex -> {
                    log.error("Erreur lors du stockage du fichier de composition", ex);
                    return new FileProcessingException("Impossible de stocker le fichier: " + fileName, ex);
                });
            
        } catch (Exception ex) {
            return Mono.error(new FileProcessingException("Impossible de stocker le fichier: " + fileName, ex));
        }
    }

    /**
     * Charge un fichier en tant que Resource (Réactif)
     */
    @Cacheable(value = "files", key = "#fileType + '_' + #fileName")
    public Mono<Resource> loadFileAsResource(String fileName, String fileType) {
        return Mono.fromCallable(() -> {
            Path filePath = getFilePathByType(fileType, fileName);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                log.debug("Fichier chargé: {}", fileName);
                return resource;
            } else {
                log.warn("Fichier introuvable ou illisible: {}", fileName);
                throw new FileProcessingException("Fichier introuvable: " + fileName);
            }
        }).onErrorMap(MalformedURLException.class, ex -> {
            log.error("Erreur lors du chargement du fichier: {}", fileName, ex);
            return new FileProcessingException("Fichier introuvable: " + fileName, ex);
        });
    }

    /**
     * Supprime un fichier (Réactif)
     */
    @CacheEvict(value = "files", key = "#fileType + '_' + #fileName")
    public Mono<Boolean> deleteFile(String fileName, String fileType) {
        return Mono.fromCallable(() -> {
            Path filePath = getFilePathByType(fileType, fileName);
            boolean deleted = Files.deleteIfExists(filePath);
            
            if (deleted) {
                log.info("Fichier supprimé: {}", fileName);
            } else {
                log.warn("Fichier non trouvé pour suppression: {}", fileName);
            }
            
            return deleted;
        }).onErrorMap(IOException.class, ex -> {
            log.error("Erreur lors de la suppression du fichier: {}", fileName, ex);
            return new FileProcessingException("Impossible de supprimer le fichier: " + fileName, ex);
        });
    }

    /**
     * Valide une photo de profil
     */
    private void validateProfilePicture(FilePart file) {
        if (file == null) {
            throw new FileProcessingException("Le fichier est vide");
        }

        String contentType = file.headers().getContentType().toString();
        if (!fileStorageConfig.isAllowedImageType(contentType)) {
            throw new FileProcessingException(
                "Type de fichier non autorisé: " + contentType + 
                ". Types autorisés: " + fileStorageConfig.getAllowedImageTypes()
            );
        }
    }

    /**
     * Valide un document
     */
    private void validateDocument(FilePart file) {
        if (file == null) {
            throw new FileProcessingException("Le fichier est vide");
        }

        String contentType = file.headers().getContentType().toString();
        if (!fileStorageConfig.isAllowedDocumentType(contentType)) {
            throw new FileProcessingException("Type de document non autorisé: " + contentType);
        }
    }

    /**
     * Génère un nom de fichier unique
     */
    private String generateUniqueFileName(FilePart file, String prefix) {
        String originalFileName = StringUtils.cleanPath(file.filename());
        String fileExtension = "";
        
        if (originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        return prefix + "_" + UUID.randomUUID() + "_" + System.currentTimeMillis() + fileExtension;
    }

    /**
     * Retourne le chemin d'un fichier selon son type
     */
    private Path getFilePathByType(String fileType, String fileName) {
        return switch (fileType.toLowerCase()) {
            case "profile" -> fileStorageConfig.getProfilePicturesPath().resolve(fileName);
            case "composition" -> fileStorageConfig.getCompositionFilesPath().resolve(fileName);
            case "temp" -> fileStorageConfig.getTempPath().resolve(fileName);
            default -> throw new IllegalArgumentException("Type de fichier inconnu: " + fileType);
        };
    }
}
