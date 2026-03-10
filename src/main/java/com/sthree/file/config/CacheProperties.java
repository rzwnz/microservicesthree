package com.sthree.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/**
 * Configuration properties for cache settings
 * 
 * Controls TTL (Time To Live) for different cache types
 * in Redis
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    /**
     * TTL for file metadata cache in minutes.
     * Default: 60 minutes
     */
    @Min(value = 1, message = "File metadata TTL must be at least 1 minute")
    private int fileMetadataTtlMinutes = 60;

    /**
     * TTL for presigned URL cache in minutes.
     * Default: 15 minutes
     */
    @Min(value = 1, message = "Presigned URL TTL must be at least 1 minute")
    private int presignedUrlTtlMinutes = 15;
}
