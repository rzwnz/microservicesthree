package com.sthree.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sthree.file.config.FileUploadProperties;
import com.sthree.file.config.GarageProperties;
import com.sthree.file.config.RedisConfig;
import com.sthree.file.dto.ConfirmUploadRequest;
import com.sthree.file.dto.FileMetadataResponse;
import com.sthree.file.dto.FileResponse;
import com.sthree.file.dto.PresignedUploadRequest;
import com.sthree.file.dto.PresignedUploadResponse;
import com.sthree.file.entity.FileEntity;
import com.sthree.file.entity.FileThumbnailEntity;
import com.sthree.file.event.FileEventPublisher;
import com.sthree.file.exception.*;
import com.sthree.file.repository.FileAccessRepository;
import com.sthree.file.repository.FileMetadataRepository;
import com.sthree.file.repository.FileRepository;
import com.sthree.file.repository.FileThumbnailRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service for file operations.
 * 
 * Handles file upload, download, deletion, and metadata management
 * with integration to Garage storage and PostgreSQL database.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService implements IFileService {

    private final FileRepository fileRepository;
    private final FileMetadataRepository metadataRepository;
    private final FileAccessRepository accessRepository;
    private final FileThumbnailRepository thumbnailRepository;
    private final StorageService storageService;
    private final VirusScanService virusScanService;
    private final FileUploadProperties uploadProperties;
    private final GarageProperties garageProperties;
    private final FileEventPublisher eventPublisher;

    @Value("${storage.quota.default-bytes:1073741824}")
    private long defaultQuotaBytes;

    @Override
    public PresignedUploadResponse createPresignedUpload(UUID userId, PresignedUploadRequest request) {
        validatePresignedRequest(request);
        checkStorageQuota(userId, request.getFileSize());

        String category = mapTypeToCategory(request.getType());
        String storagePath = storageService.generateStoragePath(userId, category, request.getOriginalName());
        String bucketName = garageProperties.getDataBucket();
        String uploadUrl = storageService.generatePresignedUploadUrl(
            bucketName,
            storagePath,
            request.getContentType()
        );

        return PresignedUploadResponse.builder()
            .uploadUrl(uploadUrl)
            .storagePath(storagePath)
            .bucketName(bucketName)
            .expiresAt(storageService.getUploadUrlExpiration())
            .build();
        }

    @Override
    @Transactional
    @CacheEvict(value = RedisConfig.CacheNames.STORAGE_QUOTA, key = "#userId")
    public FileResponse confirmPresignedUpload(UUID userId, ConfirmUploadRequest request) {
        validateConfirmedUpload(request);
        checkStorageQuota(userId, request.getFileSize());

        if (!storageService.fileExists(request.getBucketName(), request.getStoragePath())) {
            throw new StorageException("Uploaded object not found in storage");
        }

        FileEntity.AccessLevel level = request.getAccessLevel() != null
            ? FileEntity.AccessLevel.fromString(request.getAccessLevel())
            : FileEntity.AccessLevel.PRIVATE;

        FileEntity fileEntity = FileEntity.builder()
            .fileName(extractFileName(request.getStoragePath()))
            .originalName(request.getOriginalName())
            .fileSize(request.getFileSize())
            .fileType(request.getType())
            .mimeType(request.getContentType())
            .storagePath(request.getStoragePath())
            .bucketName(request.getBucketName())
            .uploadedBy(userId)
            .accessLevel(level)
            .checksum(request.getChecksum())
            .build();

        fileEntity = fileRepository.insert(fileEntity);

        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            metadataRepository.insertBatch(fileEntity.getId(), request.getMetadata());
        }

        eventPublisher.publishFileUploaded(fileEntity, userId);

        String downloadUrl = storageService.generatePresignedDownloadUrl(
            fileEntity.getBucketName(),
            fileEntity.getStoragePath()
        );

        return FileResponse.builder()
            .fileId(fileEntity.getId())
            .fileName(fileEntity.getFileName())
            .originalName(fileEntity.getOriginalName())
            .fileSize(fileEntity.getFileSize())
            .fileType(fileEntity.getFileType())
            .mimeType(fileEntity.getMimeType())
            .url(downloadUrl)
            .accessLevel(fileEntity.getAccessLevel().getValue())
            .uploadedAt(fileEntity.getCreatedAt())
            .uploadedBy(fileEntity.getUploadedBy())
            .expiresAt(storageService.getDownloadUrlExpiration())
            .build();
        }

    /**
     * Upload a file.
     * 
     * @param file the multipart file
     * @param userId the user ID
     * @param fileType the file type (image, code, document)
     * @param accessLevel the access level
     * @param metadata additional metadata
     * @return the file response with upload details
     */
    @Override
    @Transactional
    @CacheEvict(value = RedisConfig.CacheNames.STORAGE_QUOTA, key = "#userId")
    public FileResponse uploadFile(MultipartFile file, UUID userId, String fileType,
                                   FileEntity.AccessLevel accessLevel, Map<String, String> metadata) {
        log.info("Uploading file: {} for user: {}", file.getOriginalFilename(), userId);

        // Validate file
        validateFile(file, fileType);

        // Check storage quota
        checkStorageQuota(userId, file.getSize());

        // Generate storage path
        String category = mapTypeToCategory(fileType);
        String storagePath = storageService.generateStoragePath(userId, category, file.getOriginalFilename());
        String bucketName = garageProperties.getDataBucket();

        // Calculate checksum
        String checksum = calculateChecksum(file);

        try {
            // Virus scan before upload
            VirusScanService.ScanResult scanResult = virusScanService.scan(
                    file.getInputStream(), file.getOriginalFilename());
            if (scanResult.isInfected()) {
                throw new FileServiceException(
                        "Virus detected: " + scanResult.getThreatName(),
                        FileServiceException.ErrorCode.VALIDATION_ERROR, 400);
            }

            // Upload to Garage
            storageService.uploadFile(bucketName, storagePath, file.getInputStream(),
                    file.getContentType(), file.getSize());

            // Create file entity
            FileEntity fileEntity = FileEntity.builder()
                    .fileName(extractFileName(storagePath))
                    .originalName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .fileType(fileType)
                    .mimeType(file.getContentType())
                    .storagePath(storagePath)
                    .bucketName(bucketName)
                    .uploadedBy(userId)
                    .accessLevel(accessLevel != null ? accessLevel : FileEntity.AccessLevel.PRIVATE)
                    .checksum(checksum)
                    .build();

            fileEntity = fileRepository.insert(fileEntity);

            // Store metadata
            if (metadata != null && !metadata.isEmpty()) {
                metadataRepository.insertBatch(fileEntity.getId(), metadata);
            }

            // Generate thumbnails for images
            String thumbnailUrl = null;
            if ("image".equals(fileType)) {
                thumbnailUrl = generateAndStoreThumbnails(fileEntity, file);
            }

            // Publish event
            eventPublisher.publishFileUploaded(fileEntity, userId);

            // Generate download URL
            String downloadUrl = storageService.generatePresignedDownloadUrl(bucketName, storagePath);

            log.info("File uploaded successfully: {} for user: {}", fileEntity.getId(), userId);

            return FileResponse.builder()
                    .fileId(fileEntity.getId())
                    .fileName(fileEntity.getFileName())
                    .originalName(fileEntity.getOriginalName())
                    .fileSize(fileEntity.getFileSize())
                    .fileType(fileEntity.getFileType())
                    .mimeType(fileEntity.getMimeType())
                    .url(downloadUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .accessLevel(fileEntity.getAccessLevel().getValue())
                    .uploadedAt(fileEntity.getCreatedAt())
                    .uploadedBy(fileEntity.getUploadedBy())
                    .expiresAt(storageService.getDownloadUrlExpiration())
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            throw new FileUploadException("Failed to upload file", e);
        }
    }

    /**
     * Get a file by ID.
     * 
     * @param fileId the file ID
     * @param userId the user ID (for access check)
     * @return the file response
     */
    @Cacheable(value = RedisConfig.CacheNames.FILE_METADATA, key = "#fileId + ':' + #userId")
    @Override
    public FileResponse getFile(UUID fileId, UUID userId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        // Check access
        if (!hasAccess(file, userId)) {
            throw FileAccessDeniedException.forFile(fileId, userId);
        }

        // Generate download URL
        String downloadUrl = storageService.generatePresignedDownloadUrl(
                file.getBucketName(), file.getStoragePath());

        // Get thumbnail URL
        String thumbnailUrl = getThumbnailUrl(file.getId());

        // Get metadata
        Map<String, String> metadata = metadataRepository.findMapByFileId(file.getId());

        return FileResponse.builder()
                .fileId(file.getId())
                .fileName(file.getFileName())
                .originalName(file.getOriginalName())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .mimeType(file.getMimeType())
                .url(downloadUrl)
                .thumbnailUrl(thumbnailUrl)
                .accessLevel(file.getAccessLevel().getValue())
                .uploadedAt(file.getCreatedAt())
                .uploadedBy(file.getUploadedBy())
                .metadata(metadata)
                .expiresAt(storageService.getDownloadUrlExpiration())
                .build();
    }

            @Override
            public FileMetadataResponse getFileMetadata(UUID fileId, UUID userId) {
            FileResponse file = getFile(fileId, userId);
            Map<String, String> metadata = metadataRepository.findMapByFileId(fileId);

            FileMetadataResponse.FileMetadataDetails details = FileMetadataResponse.FileMetadataDetails.builder()
                .fileName(file.getOriginalName())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .mimeType(file.getMimeType())
                .uploadedBy(file.getUploadedBy())
                .uploadedAt(file.getUploadedAt())
                .customMetadata(metadata)
                .build();

            return FileMetadataResponse.builder()
                .fileId(fileId)
                .metadata(details)
                .build();
            }

    /**
     * Download a file.
     * 
     * @param fileId the file ID
     * @param userId the user ID (for access check)
     * @return the file content as byte array
     */
    public byte[] downloadFile(UUID fileId, UUID userId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        // Check access
        if (!hasAccess(file, userId)) {
            throw FileAccessDeniedException.forFile(fileId, userId);
        }

        log.info("Downloading file: {} for user: {}", fileId, userId);
        return storageService.downloadFile(file.getBucketName(), file.getStoragePath());
    }

    /**
     * Get file input stream for streaming downloads.
     * 
     * @param fileId the file ID
     * @param userId the user ID
     * @return input stream for the file
     */
    @Override
    public InputStream getFileStream(UUID fileId, UUID userId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        // Check access
        if (!hasAccess(file, userId)) {
            throw FileAccessDeniedException.forFile(fileId, userId);
        }

        return storageService.getFileStream(file.getBucketName(), file.getStoragePath());
    }

    /**
     * Delete a file.
     * 
     * @param fileId the file ID
     * @param userId the user ID (for ownership check)
     * @return true if deleted successfully
     */
    @Transactional
        @Caching(evict = {
            @CacheEvict(value = RedisConfig.CacheNames.FILE_METADATA, allEntries = true),
            @CacheEvict(value = RedisConfig.CacheNames.PRESIGNED_URLS, allEntries = true),
            @CacheEvict(value = RedisConfig.CacheNames.STORAGE_QUOTA, key = "#userId")
        })
    @Override
    public boolean deleteFile(UUID fileId, UUID userId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        // Check ownership
        if (!file.isOwner(userId)) {
            throw FileAccessDeniedException.ownershipRequired(fileId, userId);
        }

        // Soft delete in database
        fileRepository.softDelete(fileId);

        // Delete from storage after grace period (handled by cleanup job)
        // For immediate deletion, uncomment:
        // storageService.deleteFile(file.getBucketName(), file.getStoragePath());

        // Publish event
        eventPublisher.publishFileDeleted(fileId, userId);

        log.info("File deleted: {} by user: {}", fileId, userId);
        return true;
    }

    /**
     * Get files uploaded by a user.
     * 
     * @param userId the user ID
     * @return list of files
     */
    @Override
    public List<FileResponse> getUserFiles(UUID userId) {
        List<FileEntity> files = fileRepository.findByUploadedBy(userId);
        return files.stream()
                .map(this::toFileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get storage usage for a user.
     * 
     * @param userId the user ID
     * @return storage usage information
     */
    @Cacheable(value = RedisConfig.CacheNames.STORAGE_QUOTA, key = "#userId")
    @Override
    public StorageUsage getStorageUsage(UUID userId) {
        long usedBytes = fileRepository.calculateStorageUsed(userId);
        long fileCount = fileRepository.countByUploadedBy(userId);
        return new StorageUsage(usedBytes, defaultQuotaBytes, fileCount);
    }

    /**
     * Validate a file before upload.
     */
    private void validateFile(MultipartFile file, String fileType) {
        if (file.isEmpty()) {
            throw FileValidationException.invalidFileContent("File is empty");
        }

        // Check file size
        long maxSize = getMaxFileSize(fileType);
        if (file.getSize() > maxSize) {
            throw FileValidationException.fileTooLarge(maxSize, file.getSize());
        }

        // Check file type
        String mimeType = file.getContentType();
        if (!uploadProperties.isAllowedType(mimeType)) {
            throw FileValidationException.invalidFileType(mimeType);
        }
    }

    private void validatePresignedRequest(PresignedUploadRequest request) {
        long maxSize = getMaxFileSize(request.getType());
        if (request.getFileSize() > maxSize) {
            throw FileValidationException.fileTooLarge(maxSize, request.getFileSize());
        }

        if (!uploadProperties.isAllowedType(request.getContentType())) {
            throw FileValidationException.invalidFileType(request.getContentType());
        }
    }

    private void validateConfirmedUpload(ConfirmUploadRequest request) {
        long maxSize = getMaxFileSize(request.getType());
        if (request.getFileSize() > maxSize) {
            throw FileValidationException.fileTooLarge(maxSize, request.getFileSize());
        }

        if (!uploadProperties.isAllowedType(request.getContentType())) {
            throw FileValidationException.invalidFileType(request.getContentType());
        }
    }

    /**
     * Get max file size based on file type.
     */
    private long getMaxFileSize(String fileType) {
        return switch (fileType) {
            case "image" -> uploadProperties.getMaxSizeChatAttachmentBytes();
            case "avatar" -> uploadProperties.getMaxSizeAvatarBytes();
            default -> uploadProperties.getMaxSizeBytes();
        };
    }

    /**
     * Check storage quota for a user.
     */
    private void checkStorageQuota(UUID userId, long additionalBytes) {
        long usedBytes = fileRepository.calculateStorageUsed(userId);
        if (usedBytes + additionalBytes > defaultQuotaBytes) {
            throw new QuotaExceededException(usedBytes, defaultQuotaBytes, additionalBytes);
        }
    }

    /**
     * Calculate SHA-256 checksum of a file.
     */
    private String calculateChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            log.warn("Failed to calculate checksum", e);
            return null;
        }
    }

    /**
     * Map a user-facing file type to a domain-agnostic storage category.
     *
     * @param fileType user-facing type: image, avatar, code, document, group, etc.
     * @return neutral category for {@link com.sthree.file.util.StorageKeyBuilder}
     */
    private String mapTypeToCategory(String fileType) {
        if (fileType == null) return "files";
        return switch (fileType.toLowerCase()) {
            case "avatar", "image", "group" -> "media";
            case "code", "document" -> "attachments";
            default -> "files";
        };
    }

    /**
     * Extract filename from storage path.
     */
    private String extractFileName(String storagePath) {
        if (storagePath == null) return null;
        int lastSlash = storagePath.lastIndexOf('/');
        return lastSlash >= 0 ? storagePath.substring(lastSlash + 1) : storagePath;
    }

    /**
     * Generate and store thumbnails for an image.
     */
    private String generateAndStoreThumbnails(FileEntity fileEntity, MultipartFile file) {
        try {
            // Generate medium thumbnail
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Thumbnails.of(file.getInputStream())
                    .size(300, 300)
                    .outputQuality(0.8)
                    .toOutputStream(outputStream);

            byte[] thumbnailData = outputStream.toByteArray();
            String thumbnailPath = fileEntity.getStoragePath() + "/thumbnail_medium";

            storageService.uploadFile(
                    fileEntity.getBucketName(),
                    thumbnailPath,
                    new ByteArrayInputStream(thumbnailData),
                    fileEntity.getMimeType(),
                    thumbnailData.length
            );

            // Save thumbnail metadata
            FileThumbnailEntity thumbnail = FileThumbnailEntity.of(
                    fileEntity.getId(),
                    thumbnailPath,
                    FileThumbnailEntity.ThumbnailSize.MEDIUM,
                    300,
                    300
            );
            thumbnailRepository.insert(thumbnail);

            return storageService.generatePresignedDownloadUrl(
                    fileEntity.getBucketName(), thumbnailPath);

        } catch (IOException e) {
            log.warn("Failed to generate thumbnail for file: {}", fileEntity.getId(), e);
            return null;
        }
    }

    /**
     * Get thumbnail URL for a file.
     */
    private String getThumbnailUrl(UUID fileId) {
        return thumbnailRepository.findByFileIdAndSize(fileId, FileThumbnailEntity.ThumbnailSize.MEDIUM)
                .map(thumbnail -> storageService.generatePresignedDownloadUrl(
                        garageProperties.getDataBucket(),
                        thumbnail.getThumbnailPath()))
                .orElse(null);
    }

    /**
     * Check if a user has access to a file.
     */
    private boolean hasAccess(FileEntity file, UUID userId) {
        // Owner always has access
        if (file.isOwner(userId)) {
            return true;
        }

        // Public files are accessible to all authenticated users
        if (file.getAccessLevel() == FileEntity.AccessLevel.PUBLIC) {
            return true;
        }

        // Check explicit access
        return accessRepository.hasAnyAccess(file.getId(), userId);
    }

    /**
     * Convert entity to response DTO.
     */
    private FileResponse toFileResponse(FileEntity entity) {
        return FileResponse.fromEntity(entity);
    }

    /**
     * Storage usage information.
     */
    public record StorageUsage(long usedBytes, long quotaBytes, long fileCount) {
        public double getUsagePercent() {
            return (double) usedBytes / quotaBytes * 100;
        }
    }
}
