package com.sthree.file.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;

/**
 * Configuration for Garage (S3-compatible) client.
 * 
 * Creates and configures the AWS S3 SDK client to work with Garage,
 * an open-source S3-compatible object storage service.
 * 
 * Garage uses path-style URLs and may have different endpoint configurations
 * than AWS S3, which this configuration handles appropriately.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GarageConfig {

    private final GarageProperties garageProperties;

    /**
     * Create and configure the S3Client for Garage.
     * 
     * The client is configured with:
     * - Custom endpoint pointing to Garage instance
     * - Path-style access (required for Garage)
     * - Credentials from configuration
     * - Appropriate timeouts
     * 
     * @return configured S3Client instance
     */
    @Bean
    public S3Client s3Client() {
        log.info("Configuring S3 client for Garage at endpoint: {}", garageProperties.getEndpoint());

        // Create credentials provider
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                garageProperties.getAccessKey(),
                garageProperties.getSecretKey()
        );

        // Configure S3 for Garage compatibility
        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(garageProperties.isPathStyleAccess())
                .chunkedEncodingEnabled(false)
                .build();

        // Create the client
        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(garageProperties.getEndpoint()))
                .region(Region.of(garageProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(s3Configuration)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMinutes(5))
                        .apiCallAttemptTimeout(Duration.ofMinutes(2))
                        .build())
                .build();

        log.info("S3 client configured successfully for Garage");
        return client;
    }

    /**
     * Create and configure the S3Presigner for generating presigned URLs.
     * 
     * Thread-safe singleton managed by Spring.
     * Automatically closed on shutdown via destroyMethod.
     * 
     * @return configured S3Presigner instance
     */
    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner() {
        log.info("Configuring S3 presigner for Garage at endpoint: {}", garageProperties.getEndpoint());

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                garageProperties.getAccessKey(),
                garageProperties.getSecretKey()
        );

        return S3Presigner.builder()
                .endpointOverride(URI.create(garageProperties.getEndpoint()))
                .region(Region.of(garageProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
