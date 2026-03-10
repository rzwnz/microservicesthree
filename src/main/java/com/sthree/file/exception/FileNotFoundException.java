package com.sthree.file.exception;

import java.util.UUID;

/**
 * Exception thrown when a file is not found.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
public class FileNotFoundException extends FileServiceException {

    /**
     * Create a new FileNotFoundException with the file ID.
     */
    public FileNotFoundException(UUID fileId) {
        super("File not found: " + fileId, ErrorCode.FILE_NOT_FOUND, 404);
    }

    /**
     * Create a new FileNotFoundException with a custom message.
     */
    public FileNotFoundException(String message) {
        super(message, ErrorCode.FILE_NOT_FOUND, 404);
    }
}
