package com.sthree.file.exception;

/**
 * Exception thrown when file validation fails.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
public class FileValidationException extends FileServiceException {

    /**
     * Create a new FileValidationException with a message.
     */
    public FileValidationException(String message) {
        super(message, ErrorCode.VALIDATION_ERROR, 400);
    }

    /**
     * Create a new FileValidationException for file too large.
     */
    public static FileValidationException fileTooLarge(long maxSize, long actualSize) {
        return new FileValidationException(
                String.format("File size exceeds maximum allowed size. Max: %d bytes, Actual: %d bytes", 
                        maxSize, actualSize));
    }

    /**
     * Create a new FileValidationException for invalid file type.
     */
    public static FileValidationException invalidFileType(String mimeType) {
        return new FileValidationException(
                String.format("File type '%s' is not allowed", mimeType));
    }

    /**
     * Create a new FileValidationException for invalid file content.
     */
    public static FileValidationException invalidFileContent(String reason) {
        return new FileValidationException("Invalid file content: " + reason);
    }
}
