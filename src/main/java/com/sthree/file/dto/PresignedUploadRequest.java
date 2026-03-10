package com.sthree.file.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for generating a presigned upload URL.
 * 
 * The client requests a presigned URL, then uploads the file
 * directly to S3/Garage storage without proxying through the server.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUploadRequest {

    /**
     * Original file name.
     */
    @NotBlank(message = "Original file name is required")
    private String originalName;

    /**
     * File type: image, avatar, code, document.
     */
    @NotBlank(message = "File type is required")
    private String type;

    /**
     * MIME content type (e.g. image/jpeg).
     */
    @NotBlank(message = "Content type is required")
    private String contentType;

    /**
     * File size in bytes.
     */
    @Min(value = 1, message = "File size must be at least 1 byte")
    private long fileSize;

    /**
     * Desired access level (public, private, shared). Defaults to private.
     */
    private String accessLevel;
}
