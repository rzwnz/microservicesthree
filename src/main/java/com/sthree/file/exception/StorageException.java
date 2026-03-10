package com.sthree.file.exception;

/**
 * Exception thrown when a storage operation fails.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
public class StorageException extends FileServiceException {

    /**
     * Create a new StorageException with a message.
     */
    public StorageException(String message) {
        super(message, ErrorCode.STORAGE_ERROR, 500);
    }

    /**
     * Create a new StorageException with a message and cause.
     */
    public StorageException(String message, Throwable cause) {
        super(message, ErrorCode.STORAGE_ERROR, 500, cause);
    }

    /**
     * Create a new StorageException for download failure.
     */
    public static StorageException downloadFailed(String fileName, Throwable cause) {
        return new StorageException("Failed to download file: " + fileName, cause);
    }

    /**
     * Create a new StorageException for upload failure.
     */
    public static StorageException uploadFailed(String fileName, Throwable cause) {
        return new StorageException("Failed to upload file: " + fileName, cause);
    }

    /**
     * Create a new StorageException for delete failure.
     */
    public static StorageException deleteFailed(String fileName, Throwable cause) {
        return new StorageException("Failed to delete file: " + fileName, cause);
    }
}
