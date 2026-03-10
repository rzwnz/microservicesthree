package com.sthree.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sthree.file.config.GarageProperties;
import com.sthree.file.dto.FileShareRequest;
import com.sthree.file.dto.FileShareResponse;
import com.sthree.file.entity.FileEntity;
import com.sthree.file.entity.FileShareEntity;
import com.sthree.file.event.FileEventPublisher;
import com.sthree.file.exception.FileAccessDeniedException;
import com.sthree.file.exception.FileNotFoundException;
import com.sthree.file.exception.FileShareException;
import com.sthree.file.repository.FileRepository;
import com.sthree.file.repository.FileShareRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for file sharing operations.
 * 
 * Handles creation and management of shareable links for files,
 * including password protection and expiration.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileShareService {

    private final FileRepository fileRepository;
    private final FileShareRepository shareRepository;
    private final StorageService storageService;
    private final GarageProperties garageProperties;
    private final FileEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a shareable link for a file.
     * 
     * @param fileId the file ID
     * @param userId the user ID (owner)
     * @param request the share request
     * @return the share response with link details
     */
    @Transactional
    public FileShareResponse createShare(UUID fileId, UUID userId, FileShareRequest request) {
        // Find file
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        // Check ownership
        if (!file.isOwner(userId)) {
            throw FileAccessDeniedException.ownershipRequired(fileId, userId);
        }

        // Generate share token
        String shareToken = generateShareToken();

        // Hash password if provided
        String passwordHash = null;
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            passwordHash = passwordEncoder.encode(request.getPassword());
        }

        // Determine access level
        FileEntity.AccessLevel accessLevel = FileEntity.AccessLevel.PUBLIC;
        if (request.getAccessLevel() != null) {
            accessLevel = FileEntity.AccessLevel.fromString(request.getAccessLevel());
        }

        // Create share entity
        FileShareEntity share = FileShareEntity.builder()
                .fileId(fileId)
                .shareToken(shareToken)
                .sharedBy(userId)
                .accessLevel(accessLevel)
                .passwordHash(passwordHash)
                .expiresAt(request.getExpiresAt())
                .maxDownloads(request.getMaxDownloads())
                .downloadCount(0)
                .build();

        share = shareRepository.insert(share);

        // Update file access level if needed
        if (file.getAccessLevel() != accessLevel) {
            fileRepository.updateAccessLevel(fileId, accessLevel);
        }

        // Publish event
        eventPublisher.publishFileShared(fileId, userId, accessLevel);

        log.info("Created share for file: {} with token: {}", fileId, shareToken);

        return FileShareResponse.builder()
                .shareToken(shareToken)
                .shareUrl(buildShareUrl(shareToken))
                .expiresAt(share.getExpiresAt())
                .maxDownloads(share.getMaxDownloads())
                .passwordProtected(passwordHash != null)
                .build();
    }

    /**
     * Access a shared file.
     * 
     * @param shareToken the share token
     * @param password optional password for protected shares
     * @return the file download URL
     */
    @Transactional
    public String accessSharedFile(String shareToken, String password) {
        // Find share
        FileShareEntity share = shareRepository.findByToken(shareToken)
                .orElseThrow(() -> FileShareException.invalidToken(shareToken));

        // Check if expired
        if (share.isExpired()) {
            throw FileShareException.expired(shareToken);
        }

        // Check download limit
        if (share.isDownloadLimitReached()) {
            throw FileShareException.limitReached(shareToken);
        }

        // Check password if protected
        if (share.isPasswordProtected()) {
            if (password == null || !passwordEncoder.matches(password, share.getPasswordHash())) {
                throw FileShareException.invalidPassword();
            }
        }

        // Find file
        FileEntity file = fileRepository.findById(share.getFileId())
                .orElseThrow(() -> new FileNotFoundException(share.getFileId()));

        // Increment download count
        shareRepository.incrementDownloadCount(shareToken);

        log.info("Accessed shared file: {} via token: {}", file.getId(), shareToken);

        // Generate download URL
        return storageService.generatePresignedDownloadUrl(file.getBucketName(), file.getStoragePath());
    }

    /**
     * Get shared file info without downloading.
     * 
     * @param shareToken the share token
     * @return file information
     */
    public FileEntity getSharedFileInfo(String shareToken) {
        FileShareEntity share = shareRepository.findByToken(shareToken)
                .orElseThrow(() -> FileShareException.invalidToken(shareToken));

        if (share.isExpired()) {
            throw FileShareException.expired(shareToken);
        }

        return fileRepository.findById(share.getFileId())
                .orElseThrow(() -> new FileNotFoundException(share.getFileId()));
    }

    /**
     * Revoke a share link.
     * 
     * @param shareToken the share token
     * @param userId the user ID (owner)
     * @return true if revoked successfully
     */
    @Transactional
    public boolean revokeShare(String shareToken, UUID userId) {
        FileShareEntity share = shareRepository.findByToken(shareToken)
                .orElseThrow(() -> FileShareException.invalidToken(shareToken));

        // Check ownership
        if (!share.getSharedBy().equals(userId)) {
            throw new FileAccessDeniedException("Only the share creator can revoke it");
        }

        shareRepository.deleteByToken(shareToken);
        log.info("Revoked share: {}", shareToken);
        return true;
    }

    /**
     * Get all shares for a file.
     * 
     * @param fileId the file ID
     * @param userId the user ID (owner)
     * @return list of shares
     */
    public List<FileShareResponse> getFileShares(UUID fileId, UUID userId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        if (!file.isOwner(userId)) {
            throw FileAccessDeniedException.ownershipRequired(fileId, userId);
        }

        return shareRepository.findByFileId(fileId).stream()
                .map(this::toShareResponse)
                .toList();
    }

    /**
     * Get all shares created by a user.
     * 
     * @param userId the user ID
     * @return list of shares
     */
    public List<FileShareResponse> getUserShares(UUID userId) {
        return shareRepository.findBySharedBy(userId).stream()
                .map(this::toShareResponse)
                .toList();
    }

    /**
     * Check if a share is valid.
     * 
     * @param shareToken the share token
     * @return true if valid
     */
    public boolean isShareValid(String shareToken) {
        return shareRepository.findByToken(shareToken)
                .map(FileShareEntity::isValid)
                .orElse(false);
    }

    /**
     * Generate a unique share token.
     */
    private String generateShareToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Build the full share URL.
     */
    private String buildShareUrl(String shareToken) {
        return String.format("/api/files/share/%s", shareToken);
    }

    /**
     * Convert share entity to response.
     */
    private FileShareResponse toShareResponse(FileShareEntity share) {
        return FileShareResponse.builder()
                .shareToken(share.getShareToken())
                .shareUrl(buildShareUrl(share.getShareToken()))
                .expiresAt(share.getExpiresAt())
                .maxDownloads(share.getMaxDownloads())
                .passwordProtected(share.isPasswordProtected())
                .build();
    }
}
