package com.sthree.file.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO to confirm a direct-to-storage (presigned) upload.
 * 
 * After the client PUT-s the file using the presigned URL,
 * it calls /confirm-upload with the storage coordinates and
 * file metadata so the server can persist the record in PostgreSQL.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmUploadRequest {

    /**
     * Storage path (object key) returned by /presigned-upload.
     */
    @NotBlank(message = "Storage path is required")
    private String storagePath;

    /**
     * Bucket name returned by /presigned-upload.
     */
    @NotBlank(message = "Bucket name is required")
    private String bucketName;

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
     * Access level: public, private, shared. Defaults to private.
     */
    private String accessLevel;

    /**
     * SHA-256 checksum computed client-side.
     */
    private String checksum;

    /**
     * Optional key-value metadata to store alongside the file record.
     */
    private Map<String, String> metadata;
}
