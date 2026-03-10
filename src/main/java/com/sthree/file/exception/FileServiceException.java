package com.sthree.file.exception;

import lombok.Getter;

/**
 * Base exception class for file service errors.
 * 
 * Provides a common base for all file service related exceptions
 * with error codes for consistent error handling.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Getter
public class FileServiceException extends RuntimeException {

    /**
     * Error code for categorizing the error.
     */
    private final String errorCode;

    /**
     * HTTP status code for the error.
     */
    private final int statusCode;

    /**
     * Create a new FileServiceException with message and error code.
     */
    public FileServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = 500;
    }

    /**
     * Create a new FileServiceException with message, error code, and status.
     */
    public FileServiceException(String message, String errorCode, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    /**
     * Create a new FileServiceException with message, error code, and cause.
     */
    public FileServiceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.statusCode = 500;
    }

    /**
     * Create a new FileServiceException with message, error code, status, and cause.
     */
    public FileServiceException(String message, String errorCode, int statusCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    /**
     * Common error codes used in the file service.
     */
    public static class ErrorCode {
        public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";
        public static final String FILE_ALREADY_EXISTS = "FILE_ALREADY_EXISTS";
        public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
        public static final String INVALID_FILE_TYPE = "INVALID_FILE_TYPE";
        public static final String INVALID_FILE_CONTENT = "INVALID_FILE_CONTENT";
        public static final String ACCESS_DENIED = "ACCESS_DENIED";
        public static final String UPLOAD_FAILED = "UPLOAD_FAILED";
        public static final String DOWNLOAD_FAILED = "DOWNLOAD_FAILED";
        public static final String DELETE_FAILED = "DELETE_FAILED";
        public static final String STORAGE_ERROR = "STORAGE_ERROR";
        public static final String SHARE_EXPIRED = "SHARE_EXPIRED";
        public static final String SHARE_LIMIT_REACHED = "SHARE_LIMIT_REACHED";
        public static final String INVALID_SHARE_TOKEN = "INVALID_SHARE_TOKEN";
        public static final String INVALID_PASSWORD = "INVALID_PASSWORD";
        public static final String QUOTA_EXCEEDED = "QUOTA_EXCEEDED";
        public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
        public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    }
}
