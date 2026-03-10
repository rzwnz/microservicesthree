package com.sthree.file.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.sthree.file.dto.*;
import com.sthree.file.entity.FileEntity;
import com.sthree.file.exception.UnauthorizedException;
import com.sthree.file.service.FileService;
import com.sthree.file.service.IFileService;
import com.sthree.file.service.FileShareService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for file operations.
 * 
 * Provides endpoints for file upload, download, deletion, and metadata management.
 * All endpoints require authentication (Bearer token).
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload, download, sharing, and metadata management")
public class FileController {

    private final IFileService fileService;
    private final FileShareService shareService;

    /**
     * Upload a file.
     * 
     * POST /api/files/upload
     * Content-Type: multipart/form-data
     * 
     * @param file the file to upload
     * @param type the file type (image, code, document)
     * @param accessLevel the access level (public, private, shared)
     * @param userId the authenticated user ID (from token)
     * @return file upload response
     */
    @Operation(summary = "Upload a file", description = "Upload a file via multipart/form-data with type and access level.")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "image") String type,
            @RequestParam(value = "accessLevel", defaultValue = "private") String accessLevel,
            @RequestAttribute(value = "userId", required = false) UUID userId) {

        userId = requireAuthenticatedUser(userId);

        log.info("Uploading file: {} for user: {}", file.getOriginalFilename(), userId);

        FileEntity.AccessLevel level = FileEntity.AccessLevel.fromString(accessLevel);
        FileResponse response = fileService.uploadFile(file, userId, type, level, null);

        return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", response));
    }

    /**
     * Get file information.
     * 
     * GET /api/files/{fileId}
     * 
     * @param fileId the file ID
     * @param userId the authenticated user ID
     * @return file information
     */
    @Operation(summary = "Get file info", description = "Retrieve file metadata and a presigned download URL.")
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileResponse>> getFile(
            @PathVariable UUID fileId,
            @RequestAttribute(value = "userId", required = false) UUID userId) {

        userId = requireAuthenticatedUser(userId);

        log.info("Getting file: {} for user: {}", fileId, userId);

        FileResponse response = fileService.getFile(fileId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Download a file.
     * 
     * GET /api/files/{fileId}/download
     * 
     * @param fileId the file ID
     * @param userId the authenticated user ID
     * @return file content
     */
    @Operation(summary = "Download a file", description = "Stream the file content as an attachment.")
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID fileId,
            @RequestAttribute(value = "userId", required = false) UUID userId) {

        userId = requireAuthenticatedUser(userId);

        log.info("Downloading file: {} for user: {}", fileId, userId);

        // Get file info first for headers
        FileResponse fileResponse = fileService.getFile(fileId, userId);

        // Get file stream
        InputStream inputStream = fileService.getFileStream(fileId, userId);
        InputStreamResource resource = new InputStreamResource(inputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileResponse.getMimeType()))
                .contentLength(fileResponse.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileResponse.getOriginalName())
                                .build().toString())
                .body(resource);
    }

    /**
     * Delete a file.
     * 
     * DELETE /api/files/{fileId}
     * 
     * @param fileId the file ID
     * @param userId the authenticated user ID
     * @return deletion result
     */
    @Operation(summary = "Delete a file", description = "Soft-delete a file. Only the owner can delete.")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> deleteFile(
            @PathVariable UUID fileId,
            @RequestAttribute(value = "userId", required = false) UUID userId) {

        userId = requireAuthenticatedUser(userId);

        log.info("Deleting file: {} for user: {}", fileId, userId);

        boolean deleted = fileService.deleteFile(fileId, userId);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully",
                Map.of("deleted", deleted)));
    }

    /**
     * Get file metadata.
     * 
     * GET /api/files/{fileId}/metadata
     * 
     * @param fileId the file ID
     * @param userId the authenticated user ID
     * @return file metadata
     */
    @Operation(summary = "Get file metadata", description = "Retrieve custom key-value metadata for a file.")
    @GetMapping("/{fileId}/metadata")
    public ResponseEntity<ApiResponse<FileMetadataResponse>> getFileMetadata(
            @PathVariable UUID fileId,
            @RequestAttribute(value = "userId", required = false) UUID userId) {

        userId = requireAuthenticatedUser(userId);

        log.info("Getting metadata for file: {} for user: {}", fileId, userId);

        FileMetadataResponse response = fileService.getFileMetadata(fileId, userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Share a file.
     * 
     * POST /api/files/{fileId}/share
     * 
     * @param fileId the file ID
     * @param request the share request
     * @param userId the authenticated user ID
     * @return share information
     */
    @Operation(summary = "Share a file", description = "Create a shareable link with optional password and expiration.")
    @PostMapping("/{fileId}/share")
    public ResponseEntity<ApiResponse<FileShareResponse>> shareFile(
            @PathVariable UUID fileId,
            @Valid @RequestBody FileShareRequest request,
            @RequestAttribute(value = "userId", required = false) UUID userId) {

        userId = requireAuthenticatedUser(userId);

        log.info("Sharing file: {} for user: {}", fileId, userId);

        FileShareResponse response = shareService.createShare(fileId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("File shared successfully", response));
    }

    /**
     * Access a shared file.
     * 
     * GET /api/files/share/{shareToken}
     * 
     * @param shareToken the share token
     * @param password optional password for protected shares
     * @return file information with download URL
     */
    @Operation(summary = "Access shared file", description = "Retrieve a shared file via its share token.")
    @GetMapping("/share/{shareToken}")
    public ResponseEntity<ApiResponse<FileResponse>> getSharedFile(
            @PathVariable String shareToken,
            @RequestParam(value = "password", required = false) String password) {

        log.info("Accessing shared file with token: {}", shareToken);

        // Get file info
        FileEntity file = shareService.getSharedFileInfo(shareToken);

        // Get download URL
        String downloadUrl = shareService.accessSharedFile(shareToken, password);

        FileResponse response = FileResponse.builder()
                .fileId(file.getId())
                .fileName(file.getFileName())
                .originalName(file.getOriginalName())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .mimeType(file.getMimeType())
                .url(downloadUrl)
                .accessLevel(file.getAccessLevel() != null ? file.getAccessLevel().getValue() : null)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Download a shared file.
     * 
     * GET /api/files/share/{shareToken}/download
     * 
     * @param shareToken the share token
     * @param password optional password
     * @return file content
     */
    @Operation(summary = "Download shared file", description = "Download a shared file; redirects to presigned URL.")
    @GetMapping("/share/{shareToken}/download")
    public ResponseEntity<Resource> downloadSharedFile(
            @PathVariable String shareToken,
            @RequestParam(value = "password", required = false) String password) {

        log.info("Downloading shared file with token: {}", shareToken);

        // Get file info
        FileEntity file = shareService.getSharedFileInfo(shareToken);

        // Access and get download URL (this also increments download count)
        String downloadUrl = shareService.accessSharedFile(shareToken, password);

        // For simplicity, return redirect to the presigned URL
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, downloadUrl)
                .build();
    }

    /**
     * Revoke a share.
     * 
     * DELETE /api/files/share/{shareToken}
     * 
     * @param shareToken the share token
     * @param userId the authenticated user ID
     * @return deletion result
     */
    @Operation(summary = "Revoke a share", description = "Revoke a share link. Only the file owner can revoke.")
    @DeleteMapping("/share/{shareToken}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> revokeShare(
            @PathVariable String shareToken,
            @RequestAttribute(value = "userId", required = false) UUID userId) {

        userId = requireAuthenticatedUser(userId);

        log.info("Revoking share: {} for user: {}", shareToken, userId);

        boolean revoked = shareService.revokeShare(shareToken, userId);
        return ResponseEntity.ok(ApiResponse.success("Share revoked successfully",
                Map.of("revoked", revoked)));
    }

    /**
     * Generate direct-to-storage presigned upload URL.
     */
    @Operation(summary = "Get presigned upload URL", description = "Generate a presigned PUT URL so the client can upload directly to Garage/S3.")
    @PostMapping("/presigned-upload")
    public ResponseEntity<ApiResponse<PresignedUploadResponse>> createPresignedUpload(
            @Valid @RequestBody PresignedUploadRequest request,
            @RequestAttribute(value = "userId", required = false) UUID userId) {

        userId = requireAuthenticatedUser(userId);

        PresignedUploadResponse response = fileService.createPresignedUpload(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Presigned upload URL generated", response));
    }

    /**
     * Confirm direct upload and persist metadata in PostgreSQL.
     */
    @Operation(summary = "Confirm presigned upload", description = "After the client uploads via presigned URL, call this to persist metadata.")
    @PostMapping("/confirm-upload")
    public ResponseEntity<ApiResponse<FileResponse>> confirmUpload(
            @Valid @RequestBody ConfirmUploadRequest request,
            @RequestAttribute(value = "userId", required = false) UUID userId) {

        userId = requireAuthenticatedUser(userId);

        FileResponse response = fileService.confirmPresignedUpload(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Upload confirmed", response));
    }

    /**
     * Get user's files.
     * 
     * GET /api/files/user/{userId}
     * 
     * @param userId the user ID
     * @return list of files
     */
    @Operation(summary = "List user files", description = "Get all files uploaded by the authenticated user.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<java.util.List<FileResponse>>> getUserFiles(
            @PathVariable UUID userId,
            @RequestAttribute(value = "userId", required = false) UUID authenticatedUserId) {

        authenticatedUserId = requireAuthenticatedUser(authenticatedUserId);
        requireSameUser(authenticatedUserId, userId);

        log.info("Getting files for user: {}", userId);

        java.util.List<FileResponse> files = fileService.getUserFiles(userId);
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    /**
     * Get storage usage.
     * 
     * GET /api/files/user/{userId}/storage
     * 
     * @param userId the user ID
     * @return storage usage information
     */
    @Operation(summary = "Get storage usage", description = "Get storage usage and quota information for the authenticated user.")
    @GetMapping("/user/{userId}/storage")
    public ResponseEntity<ApiResponse<FileService.StorageUsage>> getStorageUsage(
            @PathVariable UUID userId,
            @RequestAttribute(value = "userId", required = false) UUID authenticatedUserId) {

        authenticatedUserId = requireAuthenticatedUser(authenticatedUserId);
        requireSameUser(authenticatedUserId, userId);

        log.info("Getting storage usage for user: {}", userId);

        FileService.StorageUsage usage = fileService.getStorageUsage(userId);
        return ResponseEntity.ok(ApiResponse.success(usage));
    }

    private UUID requireAuthenticatedUser(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authenticated user is required");
        }
        return userId;
    }

    /**
     * Verify that the authenticated user matches the target user.
     * Prevents IDOR — users can only access their own data.
     */
    private void requireSameUser(UUID authenticatedUserId, UUID targetUserId) {
        if (!authenticatedUserId.equals(targetUserId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied: you can only access your own data");
        }
    }
}
