package com.sthree.file.exception;

import java.util.UUID;

/**
 * Exception thrown when storage quota is exceeded.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
public class QuotaExceededException extends FileServiceException {

    private final long usedBytes;
    private final long quotaBytes;
    private final long attemptedBytes;

    /**
     * Create a new QuotaExceededException.
     */
    public QuotaExceededException(long usedBytes, long quotaBytes, long attemptedBytes) {
        super(String.format("Storage quota exceeded. Used: %d bytes, Quota: %d bytes, Attempted: %d bytes",
                usedBytes, quotaBytes, attemptedBytes), ErrorCode.QUOTA_EXCEEDED, 413);
        this.usedBytes = usedBytes;
        this.quotaBytes = quotaBytes;
        this.attemptedBytes = attemptedBytes;
    }

    /**
     * Create a new QuotaExceededException for a user.
     */
    public static QuotaExceededException forUser(UUID userId, long usedBytes, long quotaBytes, long attemptedBytes) {
        return new QuotaExceededException(usedBytes, quotaBytes, attemptedBytes);
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public long getQuotaBytes() {
        return quotaBytes;
    }

    public long getAttemptedBytes() {
        return attemptedBytes;
    }
}
