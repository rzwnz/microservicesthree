package com.sthree.file.exception;

import java.util.UUID;

/**
 * Exception thrown when access to a file is denied.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
public class FileAccessDeniedException extends FileServiceException {

    /**
     * Create a new FileAccessDeniedException with a message.
     */
    public FileAccessDeniedException(String message) {
        super(message, ErrorCode.ACCESS_DENIED, 403);
    }

    /**
     * Create a new FileAccessDeniedException for a specific file and user.
     */
    public static FileAccessDeniedException forFile(UUID fileId, UUID userId) {
        return new FileAccessDeniedException(
                String.format("User %s does not have access to file %s", userId, fileId));
    }

    /**
     * Create a new FileAccessDeniedException for ownership requirement.
     */
    public static FileAccessDeniedException ownershipRequired(UUID fileId, UUID userId) {
        return new FileAccessDeniedException(
                String.format("User %s must be the owner of file %s to perform this action", userId, fileId));
    }
}
