package com.sthree.file.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a file stored in the system.
 * 
 * Contains metadata about files uploaded to the platform, including
 * storage location, access control, and file properties.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileEntity {

    /**
     * Unique identifier for the file.
     */
    private UUID id;

    /**
     * Unique generated filename in storage.
     */
    private String fileName;

    /**
     * Original filename as uploaded by user.
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
     * Path in object storage (Garage/S3).
     */
    private String storagePath;

    /**
     * Name of the bucket where the file is stored.
     */
    private String bucketName;

    /**
     * ID of the user who uploaded the file.
     */
    private UUID uploadedBy;

    /**
     * Access level: public, private, or shared.
     */
    private AccessLevel accessLevel;

    /**
     * SHA-256 checksum of the file content.
     */
    private String checksum;

    /**
     * Timestamp when the file was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the file was last updated.
     */
    private LocalDateTime updatedAt;

    /**
     * Timestamp for soft delete (null if not deleted).
     */
    private LocalDateTime deletedAt;

    /**
     * Enumeration of possible file types.
     */
    public enum FileType {
        IMAGE("image"),
        CODE("code"),
        DOCUMENT("document");

        private final String value;

        FileType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static FileType fromString(String value) {
            for (FileType type : FileType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown file type: " + value);
        }
    }

    /**
     * Enumeration of possible access levels.
     */
    public enum AccessLevel {
        PUBLIC("public"),
        PRIVATE("private"),
        SHARED("shared");

        private final String value;

        AccessLevel(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static AccessLevel fromString(String value) {
            for (AccessLevel level : AccessLevel.values()) {
                if (level.value.equalsIgnoreCase(value)) {
                    return level;
                }
            }
            throw new IllegalArgumentException("Unknown access level: " + value);
        }
    }

    /**
     * Check if the file is deleted (soft delete).
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Check if a user is the owner of this file.
     */
    public boolean isOwner(UUID userId) {
        return uploadedBy != null && uploadedBy.equals(userId);
    }
}
