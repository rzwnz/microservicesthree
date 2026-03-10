package com.sthree.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/**
 * Configuration properties for presigned URL settings.
 * 
 * Controls expiration times for presigned URLs used for
 * secure file uploads and downloads.
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "presigned-url")
public class PresignedUrlProperties {

    /**
     * Expiration time for upload URLs in minutes.
     */
    @Min(value = 1, message = "Upload expiration must be at least 1 minute")
    private int uploadExpirationMinutes = 30;

    /**
     * Expiration time for download URLs in minutes.
     */
    @Min(value = 1, message = "Download expiration must be at least 1 minute")
    private int downloadExpirationMinutes = 60;

    /**
     * Get upload expiration in seconds.
     */
    public long getUploadExpirationSeconds() {
        return uploadExpirationMinutes * 60L;
    }

    /**
     * Get download expiration in seconds.
     */
    public long getDownloadExpirationSeconds() {
        return downloadExpirationMinutes * 60L;
    }
}
