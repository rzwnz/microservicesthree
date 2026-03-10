package com.sthree.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.sthree.file.entity.FileEntity;

/**
 * Response DTO for file operations.
 * 
 * Contains file information returned after upload or file retrieval.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {

    /**
     * Unique identifier for the file.
     */
    private UUID fileId;

    /**
     * Generated filename in storage.
     */
    private String fileName;

    /**
     * Original filename as uploaded.
     */
    private String originalName;

    /**
     * File size in bytes.
     */
    private Long fileSize;

    /**
     * Type of file: image, code, document.
     */
    private String fileType;

    /**
     * MIME type of the file.
     */
    private String mimeType;

    /**
     * URL for downloading the file.
     */
    private String url;

    /**
     * URL for thumbnail (if image).
     */
    private String thumbnailUrl;

    /**
     * Access level of the file.
     */
    private String accessLevel;

    /**
     * Timestamp when file was uploaded.
     */
    private LocalDateTime uploadedAt;

    /**
     * ID of user who uploaded the file.
     */
    private UUID uploadedBy;

    /**
     * Additional metadata.
     */
    private Map<String, String> metadata;

    /**
     * When the download URL expires.
     */
    private LocalDateTime expiresAt;

    /**
     * Create a FileResponse from a FileEntity.
     */
    public static FileResponse fromEntity(FileEntity entity) {
        return FileResponse.builder()
                .fileId(entity.getId())
                .fileName(entity.getFileName())
                .originalName(entity.getOriginalName())
                .fileSize(entity.getFileSize())
                .fileType(entity.getFileType())
                .mimeType(entity.getMimeType())
                .accessLevel(entity.getAccessLevel() != null ? entity.getAccessLevel().getValue() : null)
                .uploadedAt(entity.getCreatedAt())
                .uploadedBy(entity.getUploadedBy())
                .build();
    }

    /**
     * Create a FileResponse with download URL.
     */
    public static FileResponse fromEntityWithUrl(FileEntity entity, String url, String thumbnailUrl) {
        FileResponse response = fromEntity(entity);
        response.setUrl(url);
        response.setThumbnailUrl(thumbnailUrl);
        return response;
    }
}
