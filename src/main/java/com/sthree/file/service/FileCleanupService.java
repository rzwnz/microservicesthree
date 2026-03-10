package com.sthree.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sthree.file.entity.FileEntity;
import com.sthree.file.repository.FileAccessRepository;
import com.sthree.file.repository.FileMetadataRepository;
import com.sthree.file.repository.FileRepository;
import com.sthree.file.repository.FileShareRepository;
import com.sthree.file.repository.FileThumbnailRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service for cleaning up expired and soft-deleted resources.
 * 
 * Handles:
 * - Permanent removal of soft-deleted files after a retention period
 * - Cleanup of expired file shares
 * - Cleanup of expired file access entries
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileCleanupService {

    private final FileRepository fileRepository;
    private final FileMetadataRepository metadataRepository;
    private final FileAccessRepository accessRepository;
    private final FileShareRepository shareRepository;
    private final FileThumbnailRepository thumbnailRepository;
    private final StorageService storageService;

    @Value("${cleanup.orphaned-files.older-than-days:30}")
    private int retentionDays;

    @Value("${cleanup.orphaned-files.enabled:true}")
    private boolean orphanedCleanupEnabled;

    @Value("${cleanup.temp-files.enabled:true}")
    private boolean tempCleanupEnabled;

    @Value("${cleanup.temp-files.older-than-hours:24}")
    private int tempFileMaxAgeHours;

    /**
     * Permanently delete soft-deleted files that are past the retention period.
     * 
     * Runs daily at 3:00 AM.
     */
    @Scheduled(cron = "${cleanup.orphaned-files.cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupSoftDeletedFiles() {
        if (!orphanedCleanupEnabled) {
            log.debug("Orphaned file cleanup is disabled");
            return;
        }

        log.info("Starting cleanup of soft-deleted files older than {} days", retentionDays);

        LocalDateTime retentionDate = LocalDateTime.now().minusDays(retentionDays);
        List<FileEntity> filesToCleanup = fileRepository.findFilesForCleanup(retentionDate);

        int successCount = 0;
        int failCount = 0;

        for (FileEntity file : filesToCleanup) {
            try {
                // Delete from Garage/S3
                storageService.deleteFile(file.getBucketName(), file.getStoragePath());

                // Delete thumbnails from storage
                thumbnailRepository.findByFileId(file.getId()).forEach(thumbnail -> {
                    try {
                        storageService.deleteFile(file.getBucketName(), thumbnail.getThumbnailPath());
                    } catch (Exception e) {
                        log.warn("Failed to delete thumbnail from storage: {}", thumbnail.getThumbnailPath(), e);
                    }
                });

                // Delete all related DB records (cascades handle most, but be explicit)
                thumbnailRepository.deleteByFileId(file.getId());
                metadataRepository.deleteByFileId(file.getId());
                accessRepository.deleteByFileId(file.getId());
                shareRepository.deleteByFileId(file.getId());
                fileRepository.hardDelete(file.getId());

                successCount++;
                log.debug("Permanently deleted file: {}", file.getId());
            } catch (Exception e) {
                failCount++;
                log.error("Failed to permanently delete file: {}", file.getId(), e);
            }
        }

        log.info("Soft-deleted file cleanup complete: {} deleted, {} failed, {} total",
                successCount, failCount, filesToCleanup.size());
    }

    /**
     * Clean up expired share links.
     * 
     * Runs every hour.
     */
    @Scheduled(cron = "${cleanup.expired-shares.cron:0 0 * * * ?}")
    public void cleanupExpiredShares() {
        log.info("Starting cleanup of expired shares");
        int deleted = shareRepository.cleanupExpired();
        log.info("Expired share cleanup complete: {} removed", deleted);
    }

    /**
     * Clean up expired access entries.
     * 
     * Runs every hour.
     */
    @Scheduled(cron = "${cleanup.expired-access.cron:0 30 * * * ?}")
    public void cleanupExpiredAccess() {
        log.info("Starting cleanup of expired access entries");
        int deleted = accessRepository.cleanupExpired();
        log.info("Expired access cleanup complete: {} removed", deleted);
    }
}
