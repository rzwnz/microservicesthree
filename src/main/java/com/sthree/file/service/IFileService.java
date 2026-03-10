package com.sthree.file.service;

import com.sthree.file.dto.ConfirmUploadRequest;
import com.sthree.file.dto.FileMetadataResponse;
import com.sthree.file.dto.FileResponse;
import com.sthree.file.dto.PresignedUploadRequest;
import com.sthree.file.dto.PresignedUploadResponse;
import com.sthree.file.entity.FileEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IFileService {
    PresignedUploadResponse createPresignedUpload(UUID userId, PresignedUploadRequest request);

    FileResponse confirmPresignedUpload(UUID userId, ConfirmUploadRequest request);

    FileResponse uploadFile(MultipartFile file, UUID userId, String fileType,
                            FileEntity.AccessLevel accessLevel, Map<String, String> metadata);

    FileResponse getFile(UUID fileId, UUID userId);

    FileMetadataResponse getFileMetadata(UUID fileId, UUID userId);

    InputStream getFileStream(UUID fileId, UUID userId);

    boolean deleteFile(UUID fileId, UUID userId);

    List<FileResponse> getUserFiles(UUID userId);

    FileService.StorageUsage getStorageUsage(UUID userId);
}