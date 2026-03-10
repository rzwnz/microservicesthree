package com.sthree.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for Garage (S3-compatible) object storage.
 * 
 * <p>Garage is an open-source distributed object storage service that implements
 * the Amazon S3 API. This class holds all configuration needed to connect
 * to a Garage instance.
 *
 * <p><strong>Bucket strategy (application-agnostic):</strong> a single
 * {@code dataBucket} holds all application data. Object keys produced by
 * {@link com.sthree.file.util.StorageKeyBuilder} encode the resource type,
 * scope and category inside the key prefix — no domain-specific buckets are
 * needed.
 * 
 * @author rzwnz
 * @version 2.0.0
 * @see com.sthree.file.util.StorageKeyBuilder
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "garage")
public class GarageProperties {

    /**
     * Garage endpoint URL (e.g., http://localhost:3900).
     */
    @NotBlank(message = "Garage endpoint is required")
    private String endpoint;

    /**
     * Access key for Garage authentication.
     */
    @NotBlank(message = "Garage access key is required")
    private String accessKey;

    /**
     * Secret key for Garage authentication.
     */
    @NotBlank(message = "Garage secret key is required")
    private String secretKey;

    /**
     * Region for Garage storage (default: garage).
     */
    @NotBlank(message = "Garage region is required")
    private String region = "garage";

    /**
     * Single data bucket that holds all application objects.
     * Key prefixes ({@code entities/}, {@code artifacts/}, {@code tmp/}, …)
     * separate concerns inside this one bucket.
     */
    @NotBlank(message = "Garage data bucket is required")
    private String dataBucket = "file-service-dev-data";

    /**
     * Whether to use path-style access (required for Garage).
     * Garage uses path-style URLs: http://endpoint/bucket/key
     * not virtual-hosted style: http://bucket.endpoint/key
     */
    private boolean pathStyleAccess = true;
}
