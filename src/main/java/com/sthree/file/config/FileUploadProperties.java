package com.sthree.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration properties for file upload settings.
 * 
 * Controls file size limits, allowed types, and validation rules
 * for different file upload scenarios.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {

    /**
     * Maximum file size in megabytes for general uploads.
     */
    @Min(value = 1, message = "Max file size must be at least 1 MB")
    private int maxSizeMb = 50;

    /**
     * Maximum file size in megabytes for avatar uploads.
     */
    @Min(value = 1, message = "Max avatar size must be at least 1 MB")
    private int maxSizeAvatarMb = 5;

    /**
     * Maximum file size in megabytes for chat attachments.
     */
    @Min(value = 1, message = "Max chat attachment size must be at least 1 MB")
    private int maxSizeChatAttachmentMb = 10;

    /**
     * Allowed image MIME types.
     */
    private List<String> allowedImageTypes = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    /**
     * Allowed document MIME types.
     */
    private List<String> allowedDocumentTypes = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    /**
     * Maximum file size in bytes for general uploads.
     */
    public long getMaxSizeBytes() {
        return maxSizeMb * 1024L * 1024L;
    }

    /**
     * Maximum file size in bytes for avatar uploads.
     */
    public long getMaxSizeAvatarBytes() {
        return maxSizeAvatarMb * 1024L * 1024L;
    }

    /**
     * Maximum file size in bytes for chat attachment uploads.
     */
    public long getMaxSizeChatAttachmentBytes() {
        return maxSizeChatAttachmentMb * 1024L * 1024L;
    }

    /**
     * Check if a MIME type is an allowed image type.
     */
    public boolean isAllowedImageType(String mimeType) {
        return allowedImageTypes.contains(mimeType);
    }

    /**
     * Check if a MIME type is an allowed document type.
     */
    public boolean isAllowedDocumentType(String mimeType) {
        return allowedDocumentTypes.contains(mimeType);
    }

    /**
     * Check if a MIME type is allowed (image or document).
     */
    public boolean isAllowedType(String mimeType) {
        return isAllowedImageType(mimeType) || isAllowedDocumentType(mimeType);
    }
}
