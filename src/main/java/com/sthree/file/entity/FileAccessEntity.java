package com.sthree.file.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing access control for a file.
 * 
 * Manages permissions for users to access files that are shared
 * or have restricted access.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileAccessEntity {

    /**
     * Unique identifier for the access entry.
     */
    private UUID id;

    /**
     * Reference to the file this access control belongs to.
     */
    private UUID fileId;

    /**
     * User ID who has access (null for public access).
     */
    private UUID userId;

    /**
     * Type of access granted: read, write, delete.
     */
    private AccessType accessType;

    /**
     * User who granted this access.
     */
    private UUID grantedBy;

    /**
     * Timestamp when access was granted.
     */
    private LocalDateTime grantedAt;

    /**
     * Timestamp when access expires (null for no expiration).
     */
    private LocalDateTime expiresAt;

    /**
     * Enumeration of access types.
     */
    public enum AccessType {
        READ("read"),
        WRITE("write"),
        DELETE("delete");

        private final String value;

        AccessType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static AccessType fromString(String value) {
            for (AccessType type : AccessType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown access type: " + value);
        }
    }

    /**
     * Check if this access entry is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Check if this access is valid (not expired).
     */
    public boolean isValid() {
        return !isExpired();
    }

    /**
     * Create a new read access entry.
     */
    public static FileAccessEntity readAccess(UUID fileId, UUID userId, UUID grantedBy) {
        return FileAccessEntity.builder()
                .id(UUID.randomUUID())
                .fileId(fileId)
                .userId(userId)
                .accessType(AccessType.READ)
                .grantedBy(grantedBy)
                .grantedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create a new write access entry.
     */
    public static FileAccessEntity writeAccess(UUID fileId, UUID userId, UUID grantedBy) {
        return FileAccessEntity.builder()
                .id(UUID.randomUUID())
                .fileId(fileId)
                .userId(userId)
                .accessType(AccessType.WRITE)
                .grantedBy(grantedBy)
                .grantedAt(LocalDateTime.now())
                .build();
    }
}
