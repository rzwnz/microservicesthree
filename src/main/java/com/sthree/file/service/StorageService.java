package com.sthree.file.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sthree.file.config.GarageProperties;
import com.sthree.file.config.PresignedUrlProperties;
import com.sthree.file.config.RedisConfig;
import com.sthree.file.exception.StorageException;
import com.sthree.file.util.StorageKeyBuilder;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for Garage (S3-compatible) storage operations.
 * 
 * Provides methods for uploading, downloading, and managing files
 * in Garage object storage using the AWS S3 SDK.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final GarageProperties garageProperties;
    private final PresignedUrlProperties presignedUrlProperties;

    /**
     * Upload a file to Garage.
     * 
     * @param bucketName the bucket name
     * @param key the object key (path)
     * @param inputStream the file content
     * @param contentType the MIME type
     * @param contentLength the file size
     * @return the storage path
     */
    @CircuitBreaker(name = "garageStorage")
    @Retry(name = "garageStorage")
    public String uploadFile(String bucketName, String key, InputStream inputStream, 
                             String contentType, long contentLength) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));

            log.info("Uploaded file to Garage: {}/{}", bucketName, key);
            return key;
        } catch (Exception e) {
            log.error("Failed to upload file to Garage: {}/{}", bucketName, key, e);
            throw StorageException.uploadFailed(key, e);
        }
    }

    /**
     * Upload a file with byte array content.
     * 
     * @param bucketName the bucket name
     * @param key the object key
     * @param content the file content
     * @param contentType the MIME type
     * @return the storage path
     */
    @CircuitBreaker(name = "garageStorage")
    @Retry(name = "garageStorage")
    public String uploadFile(String bucketName, String key, byte[] content, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(content));

            log.info("Uploaded file to Garage: {}/{}", bucketName, key);
            return key;
        } catch (Exception e) {
            log.error("Failed to upload file to Garage: {}/{}", bucketName, key, e);
            throw StorageException.uploadFailed(key, e);
        }
    }

    /**
     * Download a file from Garage.
     * 
     * @param bucketName the bucket name
     * @param key the object key
     * @return the file content as byte array
     */
    @CircuitBreaker(name = "garageStorage")
    @Retry(name = "garageStorage")
    public byte[] downloadFile(String bucketName, String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.getObjectAsBytes(request).asByteArray();
        } catch (Exception e) {
            log.error("Failed to download file from Garage: {}/{}", bucketName, key, e);
            throw StorageException.downloadFailed(key, e);
        }
    }

    /**
     * Get an input stream for a file.
     * 
     * @param bucketName the bucket name
     * @param key the object key
     * @return input stream for the file
     */
    @CircuitBreaker(name = "garageStorage")
    @Retry(name = "garageStorage")
    public InputStream getFileStream(String bucketName, String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.getObject(request);
        } catch (Exception e) {
            log.error("Failed to get file stream from Garage: {}/{}", bucketName, key, e);
            throw StorageException.downloadFailed(key, e);
        }
    }

    /**
     * Delete a file from Garage.
     * 
     * @param bucketName the bucket name
     * @param key the object key
     */
    @CircuitBreaker(name = "garageStorage")
    @Retry(name = "garageStorage")
    public void deleteFile(String bucketName, String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("Deleted file from Garage: {}/{}", bucketName, key);
        } catch (Exception e) {
            log.error("Failed to delete file from Garage: {}/{}", bucketName, key, e);
            throw StorageException.deleteFailed(key, e);
        }
    }

    /**
     * Delete multiple files from Garage.
     * 
     * @param bucketName the bucket name
     * @param keys list of object keys to delete
     */
    @CircuitBreaker(name = "garageStorage")
    @Retry(name = "garageStorage")
    public void deleteFiles(String bucketName, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        try {
            List<ObjectIdentifier> objectsToDelete = keys.stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .toList();

            Delete delete = Delete.builder()
                    .objects(objectsToDelete)
                    .build();

            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(delete)
                    .build();

            s3Client.deleteObjects(request);
            log.info("Deleted {} files from Garage: {}", keys.size(), bucketName);
        } catch (Exception e) {
            log.error("Failed to delete files from Garage: {}", bucketName, e);
            throw new StorageException("Failed to delete files", e);
        }
    }

    /**
     * Check if a file exists in Garage.
     * 
     * @param bucketName the bucket name
     * @param key the object key
     * @return true if the file exists
     */
    @CircuitBreaker(name = "garageStorage")
    public boolean fileExists(String bucketName, String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Failed to check file existence in Garage: {}/{}", bucketName, key, e);
            return false;
        }
    }

    /**
     * Get file metadata from Garage.
     * 
     * @param bucketName the bucket name
     * @param key the object key
     * @return the file metadata
     */
    @CircuitBreaker(name = "garageStorage")
    public HeadObjectResponse getFileMetadata(String bucketName, String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.headObject(request);
        } catch (NoSuchKeyException e) {
            return null;
        } catch (Exception e) {
            log.error("Failed to get file metadata from Garage: {}/{}", bucketName, key, e);
            throw new StorageException("Failed to get file metadata", e);
        }
    }

    /**
     * Generate a presigned download URL.
     * 
     * @param bucketName the bucket name
     * @param key the object key
     * @return the presigned URL
     */
    @Cacheable(value = RedisConfig.CacheNames.PRESIGNED_URLS, key = "#bucketName + ':' + #key")
    public String generatePresignedDownloadUrl(String bucketName, String key) {
        return generatePresignedDownloadUrl(bucketName, key, presignedUrlProperties.getDownloadExpirationSeconds());
    }

    /**
     * Generate a presigned download URL with custom expiration.
     * 
     * @param bucketName the bucket name
     * @param key the object key
     * @param expirationSeconds expiration time in seconds
     * @return the presigned URL
     */
    @CircuitBreaker(name = "garageStorage")
    public String generatePresignedDownloadUrl(String bucketName, String key, long expirationSeconds) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expirationSeconds))
                    .getObjectRequest(getObjectRequest)
                    .build();

            String url = s3Presigner.presignGetObject(presignRequest).url().toString();
            log.debug("Generated presigned download URL for: {}/{}", bucketName, key);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate presigned download URL: {}/{}", bucketName, key, e);
            throw new StorageException("Failed to generate presigned URL", e);
        }
    }

    /**
     * Generate a presigned upload URL.
     * 
     * @param bucketName the bucket name
     * @param key the object key
     * @param contentType the content type
     * @return the presigned URL
     */
    @CircuitBreaker(name = "garageStorage")
    public String generatePresignedUploadUrl(String bucketName, String key, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(presignedUrlProperties.getUploadExpirationSeconds()))
                    .putObjectRequest(putObjectRequest)
                    .build();

            String url = s3Presigner.presignPutObject(presignRequest).url().toString();
            log.debug("Generated presigned upload URL for: {}/{}", bucketName, key);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL: {}/{}", bucketName, key, e);
            throw new StorageException("Failed to generate presigned URL", e);
        }
    }

    /**
     * Create a bucket if it doesn't exist.
     * 
     * @param bucketName the bucket name
     */
    @CircuitBreaker(name = "garageStorage")
    public void createBucketIfNotExists(String bucketName) {
        try {
            if (!s3Client.listBuckets().buckets().stream()
                    .anyMatch(bucket -> bucket.name().equals(bucketName))) {
                CreateBucketRequest request = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build();
                s3Client.createBucket(request);
                log.info("Created bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to create bucket: {}", bucketName, e);
            throw new StorageException("Failed to create bucket: " + bucketName, e);
        }
    }

    /**
     * Generate a domain-agnostic storage key using {@link StorageKeyBuilder}.
     *
     * <p>Routes by {@code category}:
     * <ul>
     *   <li>{@code media}       → {@code entities/by-id/{entityId}/media/{uuid}.ext}</li>
     *   <li>{@code attachments} → {@code entities/by-id/{entityId}/attachments/{uuid}.ext}</li>
     *   <li>anything else       → {@code entities/by-id/{entityId}/files/{uuid}.ext}</li>
     * </ul>
     *
     * @param entityId         the owning entity (user, group, project …)
     * @param category         neutral category ({@code media}, {@code attachments}, {@code files})
     * @param originalFileName original file name (extension is preserved)
     * @return the full object key (no leading slash, no bucket)
     */
    public String generateStoragePath(UUID entityId, String category, String originalFileName) {
        if (category == null || category.isBlank()) {
            category = "files";
        }
        return StorageKeyBuilder.entityObject(entityId, category, originalFileName);
    }

    /**
     * Resolve the single data bucket name from configuration.
     *
     * @return the data bucket name
     */
    public String getDataBucket() {
        return garageProperties.getDataBucket();
    }

    /**
     * Get the expiration time for a download URL.
     * 
     * @return the expiration time
     */
    public LocalDateTime getDownloadUrlExpiration() {
        return LocalDateTime.now().plusSeconds(presignedUrlProperties.getDownloadExpirationSeconds());
    }

    /**
     * Get the expiration time for an upload URL.
     */
    public LocalDateTime getUploadUrlExpiration() {
        return LocalDateTime.now().plusSeconds(presignedUrlProperties.getUploadExpirationSeconds());
    }
}

