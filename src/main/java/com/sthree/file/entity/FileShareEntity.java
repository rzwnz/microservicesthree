package com.sthree.file.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a shareable link for a file.
 * 
 * Allows creating public or password-protected links for sharing
 * files with users who may not have accounts on the platform.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileShareEntity {

    /**
     * Unique identifier for the share entry.
     */
    private UUID id;

    /**
     * Reference to the file being shared.
     */
    private UUID fileId;

    /**
     * Unique token for the share URL.
     */
    private String shareToken;

    /**
     * User who created this share.
     */
    private UUID sharedBy;

    /**
     * Access level for the shared file.
     */
    private FileEntity.AccessLevel accessLevel;

    /**
     * Hashed password for password-protected shares.
     */
    private String passwordHash;

    /**
     * Timestamp when the share expires (null for no expiration).
     */
    private LocalDateTime expiresAt;

    /**
     * Maximum number of downloads allowed.
     */
    private Integer maxDownloads;

    /**
     * Current count of downloads.
     */
    private Integer downloadCount;

    /**
     * Timestamp when the share was created.
     */
    private LocalDateTime createdAt;

    /**
     * Check if this share is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Check if the download limit has been reached.
     */
    public boolean isDownloadLimitReached() {
        return maxDownloads != null && downloadCount != null && downloadCount >= maxDownloads;
    }

    /**
     * Check if this share is valid (not expired and under download limit).
     */
    public boolean isValid() {
        return !isExpired() && !isDownloadLimitReached();
    }

    /**
     * Check if this share is password protected.
     */
    public boolean isPasswordProtected() {
        return passwordHash != null && !passwordHash.isEmpty();
    }

    /**
     * Increment the download count.
     */
    public void incrementDownloadCount() {
        if (downloadCount == null) {
            downloadCount = 1;
        } else {
            downloadCount++;
        }
    }

    /**
     * Create a new public share for a file.
     */
    public static FileShareEntity publicShare(UUID fileId, UUID sharedBy, String shareToken) {
        return FileShareEntity.builder()
                .id(UUID.randomUUID())
                .fileId(fileId)
                .shareToken(shareToken)
                .sharedBy(sharedBy)
                .accessLevel(FileEntity.AccessLevel.PUBLIC)
                .downloadCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create a new share with expiration.
     */
    public static FileShareEntity shareWithExpiration(UUID fileId, UUID sharedBy, String shareToken, 
                                                       LocalDateTime expiresAt, Integer maxDownloads) {
        return FileShareEntity.builder()
                .id(UUID.randomUUID())
                .fileId(fileId)
                .shareToken(shareToken)
                .sharedBy(sharedBy)
                .accessLevel(FileEntity.AccessLevel.SHARED)
                .expiresAt(expiresAt)
                .maxDownloads(maxDownloads)
                .downloadCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
