package com.sthree.file.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.sthree.file.config.GarageProperties;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

/**
 * Health indicator for Garage (S3-compatible) storage connectivity.
 *
 * Reports UP when the S3 endpoint is reachable and responds to listBuckets,
 * DOWN otherwise. Includes bucket count in the health details.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GarageHealthIndicator implements HealthIndicator {

    private final S3Client s3Client;
    private final GarageProperties garageProperties;

    @Override
    public Health health() {
        try {
            ListBucketsResponse response = s3Client.listBuckets();
            int bucketCount = response.buckets().size();
            boolean dataBucketExists = response.buckets().stream()
                    .anyMatch(b -> b.name().equals(garageProperties.getDataBucket()));

            Health.Builder builder = dataBucketExists ? Health.up() : Health.down();
            return builder
                    .withDetail("endpoint", garageProperties.getEndpoint())
                    .withDetail("dataBucket", garageProperties.getDataBucket())
                    .withDetail("dataBucketExists", dataBucketExists)
                    .withDetail("totalBuckets", bucketCount)
                    .build();
        } catch (Exception e) {
            log.warn("Garage health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("endpoint", garageProperties.getEndpoint())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
