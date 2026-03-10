package com.sthree.file.exception;

/**
 * Exception thrown when file share operations fail.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
public class FileShareException extends FileServiceException {

    /**
     * Create a new FileShareException with a message and error code.
     */
    public FileShareException(String message, String errorCode) {
        super(message, errorCode, 400);
    }

    /**
     * Create a new FileShareException for expired share.
     */
    public static FileShareException expired(String shareToken) {
        return new FileShareException("Share link has expired: " + shareToken, ErrorCode.SHARE_EXPIRED);
    }

    /**
     * Create a new FileShareException for download limit reached.
     */
    public static FileShareException limitReached(String shareToken) {
        return new FileShareException("Download limit reached for share: " + shareToken, ErrorCode.SHARE_LIMIT_REACHED);
    }

    /**
     * Create a new FileShareException for invalid token.
     */
    public static FileShareException invalidToken(String shareToken) {
        return new FileShareException("Invalid share token: " + shareToken, ErrorCode.INVALID_SHARE_TOKEN);
    }

    /**
     * Create a new FileShareException for invalid password.
     */
    public static FileShareException invalidPassword() {
        return new FileShareException("Invalid password for protected share", ErrorCode.INVALID_PASSWORD);
    }
}
