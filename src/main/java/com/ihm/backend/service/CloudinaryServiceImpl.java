package com.ihm.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ihm.backend.dto.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of CloudinaryService for handling image uploads.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    // Allowed image formats
    private static final List<String> ALLOWED_FORMATS = Arrays.asList("jpg", "jpeg", "png", "webp");
    
    // Max file size: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Override
    public ImageUploadResponse uploadImage(MultipartFile file) throws IOException {
        log.info("Starting image upload process for file: {}", file.getOriginalFilename());

        // Validate file
        validateFile(file);

        // Convert MultipartFile to File
        File tempFile = convertMultipartFileToFile(file);

        try {
            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.emptyMap());
            
            log.info("Image successfully uploaded to Cloudinary with public_id: {}", uploadResult.get("public_id"));

            // Build and return response
            return ImageUploadResponse.builder()
                    .url((String) uploadResult.get("secure_url"))
                    .publicId((String) uploadResult.get("public_id"))
                    .format((String) uploadResult.get("format"))
                    .width((Integer) uploadResult.get("width"))
                    .height((Integer) uploadResult.get("height"))
                    .size(((Number) uploadResult.get("bytes")).longValue())
                    .build();

        } catch (Exception e) {
            log.error("Error uploading image to Cloudinary: {}", e.getMessage(), e);
            throw new IOException("Failed to upload image to Cloudinary: " + e.getMessage(), e);
        } finally {
            // Clean up temporary file
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Validates the uploaded file for type and size constraints.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                String.format("File size exceeds maximum allowed size of %d bytes", MAX_FILE_SIZE)
            );
        }

        // Validate file type
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }

        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_FORMATS.contains(fileExtension)) {
            throw new IllegalArgumentException(
                String.format("File type '%s' is not allowed. Allowed types: %s", 
                    fileExtension, String.join(", ", ALLOWED_FORMATS))
            );
        }

        log.debug("File validation passed for: {}", originalFilename);
    }

    /**
     * Extracts file extension from filename.
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * Converts Spring MultipartFile to java.io.File for Cloudinary upload.
     */
    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("upload-", Objects.requireNonNull(file.getOriginalFilename()));
        
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }
        
        return tempFile;
    }
}
