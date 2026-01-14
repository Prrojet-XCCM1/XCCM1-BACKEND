package com.ihm.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the response after a successful image upload to Cloudinary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {
    
    /**
     * Public URL of the uploaded image
     */
    private String url;
    
    /**
     * Cloudinary public ID for the image (used for deletions and transformations)
     */
    private String publicId;
    
    /**
     * Format of the uploaded image (jpg, png, webp, etc.)
     */
    private String format;
    
    /**
     * Width of the image in pixels
     */
    private Integer width;
    
    /**
     * Height of the image in pixels
     */
    private Integer height;
    
    /**
     * Size of the file in bytes
     */
    private Long size;
}
