package com.sthree.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for file share operations.
 * 
 * Contains the share link and token information.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileShareResponse {

    /**
     * Unique token for the share.
     */
    private String shareToken;

    /**
     * Full URL for accessing the shared file.
     */
    private String shareUrl;

    /**
     * When the share link expires.
     */
    private LocalDateTime expiresAt;

    /**
     * Maximum number of downloads allowed.
     */
    private Integer maxDownloads;

    /**
     * Whether the share is password protected.
     */
    private Boolean passwordProtected;
}
