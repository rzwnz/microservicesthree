package com.sthree.file.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a file share.
 * 
 * Contains options for sharing a file with others.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileShareRequest {

    /**
     * Access level for the share: public, private, shared.
     */
    @NotBlank(message = "Access level is required")
    private String accessLevel;

    /**
     * User IDs to share with (for shared access level).
     */
    private List<UUID> sharedWith;

    /**
     * When the share link expires.
     */
    private LocalDateTime expiresAt;

    /**
     * Optional password for password-protected shares.
     */
    private String password;

    /**
     * Maximum number of downloads allowed.
     */
    private Integer maxDownloads;
}
