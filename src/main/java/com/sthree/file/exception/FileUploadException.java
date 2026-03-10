package com.sthree.file.exception;

/**
 * Exception thrown when a file upload fails.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
public class FileUploadException extends FileServiceException {

    /**
     * Create a new FileUploadException with a message.
     */
    public FileUploadException(String message) {
        super(message, ErrorCode.UPLOAD_FAILED, 500);
    }

    /**
     * Create a new FileUploadException with a message and cause.
     */
    public FileUploadException(String message, Throwable cause) {
        super(message, ErrorCode.UPLOAD_FAILED, 500, cause);
    }
}
