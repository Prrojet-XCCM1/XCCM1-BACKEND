package com.ihm.backend.controller;

import com.ihm.backend.dto.ImageUploadResponse;
import com.ihm.backend.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for handling image upload operations.
 */
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Image Upload", description = "API for uploading images to Cloudinary")
public class ImageUploadController {

    private final CloudinaryService cloudinaryService;

    /**
     * Uploads an image to Cloudinary.
     *
     * @param file the image file to upload
     * @return ResponseEntity containing the upload result or error message
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image", description = "Uploads an image file to Cloudinary and returns the public URL")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received image upload request for file: {}", file.getOriginalFilename());
            
            ImageUploadResponse response = cloudinaryService.uploadImage(file);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid file upload attempt: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
                    
        } catch (IOException e) {
            log.error("Error during image upload: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload image. Please try again later."));
        }
    }

    /**
     * Creates a standardized error response.
     */
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        return errorResponse;
    }
}
