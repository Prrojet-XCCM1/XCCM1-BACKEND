package com.ihm.backend.service;

import com.ihm.backend.dto.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Service interface for Cloudinary image operations.
 */
public interface CloudinaryService {
    
    /**
     * Uploads an image to Cloudinary.
     *
     * @param file the image file to upload
     * @return response containing image URL and metadata
     * @throws IOException if upload fails or file is invalid
     * @throws IllegalArgumentException if file type or size is not allowed
     */
    ImageUploadResponse uploadImage(MultipartFile file) throws IOException;
}
