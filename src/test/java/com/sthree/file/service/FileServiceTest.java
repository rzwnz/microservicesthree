package com.sthree.file.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.sthree.file.config.FileUploadProperties;
import com.sthree.file.config.GarageProperties;
import com.sthree.file.dto.ConfirmUploadRequest;
import com.sthree.file.dto.FileResponse;
import com.sthree.file.dto.PresignedUploadRequest;
import com.sthree.file.dto.PresignedUploadResponse;
import com.sthree.file.entity.FileEntity;
import com.sthree.file.entity.FileThumbnailEntity;
import com.sthree.file.event.FileEventPublisher;
import com.sthree.file.exception.FileAccessDeniedException;
import com.sthree.file.exception.FileValidationException;
import com.sthree.file.exception.QuotaExceededException;
import com.sthree.file.repository.FileAccessRepository;
import com.sthree.file.repository.FileMetadataRepository;
import com.sthree.file.repository.FileRepository;
import com.sthree.file.repository.FileThumbnailRepository;
import com.sthree.file.service.FileService;
import com.sthree.file.service.StorageService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    FileRepository fileRepository;
    @Mock
    FileMetadataRepository metadataRepository;
    @Mock
    FileAccessRepository accessRepository;
    @Mock
    FileThumbnailRepository thumbnailRepository;
    @Mock
    StorageService storageService;
    @Mock
    VirusScanService virusScanService;
    @Mock
    FileUploadProperties uploadProperties;
    @Mock
    GarageProperties garageProperties;
    @Mock
    FileEventPublisher eventPublisher;

    @InjectMocks
    FileService fileService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(fileService, "defaultQuotaBytes", 1024L * 1024 * 1024);
        lenient().when(uploadProperties.isAllowedType(anyString())).thenReturn(true);
        lenient().when(uploadProperties.getMaxSizeBytes()).thenReturn(50L * 1024 * 1024); // 50MB
        lenient().when(uploadProperties.getMaxSizeChatAttachmentBytes()).thenReturn(10L * 1024 * 1024);
        lenient().when(uploadProperties.getMaxSizeAvatarBytes()).thenReturn(5L * 1024 * 1024);
        lenient().when(virusScanService.scan(any(), anyString()))
                .thenReturn(VirusScanService.ScanResult.clean());

        lenient().when(garageProperties.getDataBucket()).thenReturn("file-service-dev-data");
    }

    @Test
    void uploadFile_success_nonImage() throws Exception {
        MockMultipartFile mp = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        when(storageService.generateStoragePath(any(), anyString(), anyString())).thenReturn("entities/by-id/abc/attachments/test.txt");
        when(storageService.generatePresignedDownloadUrl(anyString(), anyString())).thenReturn("http://download.url/file");
        when(fileRepository.insert(any())).thenAnswer(inv -> {
            FileEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(LocalDateTime.now());
            return e;
        });

        FileResponse resp = fileService.uploadFile(mp, userId, "code", FileEntity.AccessLevel.PRIVATE, Map.of("k", "v"));

        assertThat(resp.getFileId()).isNotNull();
        assertThat(resp.getOriginalName()).isEqualTo("test.txt");
        assertThat(resp.getUrl()).isEqualTo("http://download.url/file");

        verify(storageService).uploadFile(eq("file-service-dev-data"), anyString(), any(), eq("text/plain"), eq((long) mp.getBytes().length));
        verify(metadataRepository).insertBatch(any(), anyMap());
        verify(eventPublisher).publishFileUploaded(any(), eq(userId));
    }

    @Test
    void uploadFile_image_generatesThumbnail() throws Exception {
        // small valid PNG (Base64) so Thumbnailator can process it in tests
        byte[] png = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAAWgmWQ0AAAAASUVORK5CYII=");
        MockMultipartFile mp = new MockMultipartFile("file", "img.png", "image/png", png);

        when(storageService.generateStoragePath(any(), anyString(), anyString())).thenReturn("entities/by-id/abc/media/img.png");
        when(storageService.generatePresignedDownloadUrl(anyString(), anyString())).thenReturn("http://download.url/img");
        when(fileRepository.insert(any())).thenAnswer(inv -> {
            FileEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(LocalDateTime.now());
            return e;
        });
        when(thumbnailRepository.insert(any())).thenAnswer(inv -> inv.getArgument(0));

        FileResponse resp = fileService.uploadFile(mp, userId, "image", FileEntity.AccessLevel.PRIVATE, null);

        assertThat(resp.getThumbnailUrl()).isNotNull();
        verify(thumbnailRepository).insert(any());
        verify(storageService, atLeastOnce()).uploadFile(anyString(), contains("thumbnail"), any(), anyString(), anyLong());
    }

    @Test
    void uploadFile_empty_throwsValidation() {
        MockMultipartFile mp = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        assertThrows(FileValidationException.class, () -> fileService.uploadFile(mp, userId, "code", FileEntity.AccessLevel.PRIVATE, null));
    }

    @Test
    void uploadFile_invalidType_throwsValidation() {
        MockMultipartFile mp = new MockMultipartFile("file", "test.bin", "application/octet-stream", "x".getBytes());
        when(uploadProperties.isAllowedType(anyString())).thenReturn(false);
        assertThrows(FileValidationException.class, () -> fileService.uploadFile(mp, userId, "code", FileEntity.AccessLevel.PRIVATE, null));
    }

    @Test
    void uploadFile_quotaExceeded_throws() {
        MockMultipartFile mp = new MockMultipartFile("file", "big.bin", "application/octet-stream", new byte[10]);
        when(fileRepository.calculateStorageUsed(userId)).thenReturn(1024L * 1024 * 1024); // already at quota
        assertThrows(QuotaExceededException.class, () -> fileService.uploadFile(mp, userId, "code", FileEntity.AccessLevel.PRIVATE, null));
    }

    @Test
    void getFile_owner_success_and_metadata_thumbnail() {
        UUID fileId = UUID.randomUUID();
        FileEntity e = FileEntity.builder()
                .id(fileId)
                .fileName("f.txt")
                .originalName("f.txt")
                .fileSize(123L)
                .fileType("code")
                .mimeType("text/plain")
                .bucketName("b")
                .storagePath("p")
                .uploadedBy(userId)
                .accessLevel(FileEntity.AccessLevel.PRIVATE)
                .createdAt(LocalDateTime.now())
                .build();

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(e));
        when(storageService.generatePresignedDownloadUrl(anyString(), anyString())).thenReturn("u");
        when(thumbnailRepository.findByFileIdAndSize(eq(fileId), any())).thenReturn(Optional.of(FileThumbnailEntity.of(fileId, "p/thumbnail", FileThumbnailEntity.ThumbnailSize.MEDIUM, 300, 300)));
        when(metadataRepository.findMapByFileId(fileId)).thenReturn(Map.of("k", "v"));

        var resp = fileService.getFile(fileId, userId);
        assertThat(resp.getFileId()).isEqualTo(fileId);
        assertThat(resp.getMetadata()).containsKey("k");
        assertThat(resp.getThumbnailUrl()).isNotNull();
    }

    @Test
    void getFile_accessDenied_throws() {
        UUID fileId = UUID.randomUUID();
        FileEntity e = FileEntity.builder()
                .id(fileId)
                .uploadedBy(UUID.randomUUID())
                .accessLevel(FileEntity.AccessLevel.PRIVATE)
                .build();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(e));
        when(accessRepository.hasAnyAccess(fileId, userId)).thenReturn(false);

        assertThrows(FileAccessDeniedException.class, () -> fileService.getFile(fileId, userId));
    }

    @Test
    void deleteFile_owner_success() {
        UUID fileId = UUID.randomUUID();
        FileEntity e = FileEntity.builder()
                .id(fileId)
                .uploadedBy(userId)
                .build();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(e));
        when(fileRepository.softDelete(fileId)).thenReturn(true);

        boolean result = fileService.deleteFile(fileId, userId);
        assertThat(result).isTrue();
        verify(eventPublisher).publishFileDeleted(fileId, userId);
    }

    @Test
    void deleteFile_notOwner_throws() {
        UUID fileId = UUID.randomUUID();
        FileEntity e = FileEntity.builder().id(fileId).uploadedBy(UUID.randomUUID()).build();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(e));
        assertThrows(FileAccessDeniedException.class, () -> fileService.deleteFile(fileId, userId));
    }

    @Test
    void getUserFiles_and_getStorageUsage() {
        UUID fileId = UUID.randomUUID();
        FileEntity e = FileEntity.builder().id(fileId).uploadedBy(userId).fileName("a").build();
        when(fileRepository.findByUploadedBy(userId)).thenReturn(List.of(e));
        when(fileRepository.calculateStorageUsed(userId)).thenReturn(12345L);
        when(fileRepository.countByUploadedBy(userId)).thenReturn(7L);

        var files = fileService.getUserFiles(userId);
        assertThat(files).hasSize(1);

        var usage = fileService.getStorageUsage(userId);
        assertThat(usage.usedBytes()).isEqualTo(12345L);
        assertThat(usage.fileCount()).isEqualTo(7L);
        assertThat(usage.getUsagePercent()).isGreaterThan(0);
    }

    @Test
    void downloadFile_success() {
        UUID fileId = UUID.randomUUID();
        FileEntity e = FileEntity.builder()
                .id(fileId)
                .uploadedBy(userId)
                .bucketName("b")
                .storagePath("p")
                .accessLevel(FileEntity.AccessLevel.PRIVATE)
                .build();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(e));
        when(storageService.downloadFile("b", "p")).thenReturn("data".getBytes());

        byte[] result = fileService.downloadFile(fileId, userId);
        assertThat(result).isEqualTo("data".getBytes());
    }

    @Test
    void downloadFile_accessDenied_throws() {
        UUID fileId = UUID.randomUUID();
        FileEntity e = FileEntity.builder()
                .id(fileId)
                .uploadedBy(UUID.randomUUID())
                .accessLevel(FileEntity.AccessLevel.PRIVATE)
                .build();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(e));
        when(accessRepository.hasAnyAccess(fileId, userId)).thenReturn(false);
        assertThrows(FileAccessDeniedException.class, () -> fileService.downloadFile(fileId, userId));
    }

    @Test
    void getFileStream_success() {
        UUID fileId = UUID.randomUUID();
        FileEntity e = FileEntity.builder()
                .id(fileId)
                .uploadedBy(userId)
                .bucketName("b")
                .storagePath("p")
                .accessLevel(FileEntity.AccessLevel.PRIVATE)
                .build();
        ByteArrayInputStream mockStream = new ByteArrayInputStream("data".getBytes());
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(e));
        when(storageService.getFileStream("b", "p")).thenReturn(mockStream);

        InputStream result = fileService.getFileStream(fileId, userId);
        assertThat(result).isNotNull();
    }

    @Test
    void getFile_publicFile_accessGranted() {
        UUID fileId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        FileEntity e = FileEntity.builder()
                .id(fileId)
                .fileName("f.txt")
                .originalName("f.txt")
                .fileSize(100L)
                .fileType("code")
                .mimeType("text/plain")
                .bucketName("b")
                .storagePath("p")
                .uploadedBy(otherUser)
                .accessLevel(FileEntity.AccessLevel.PUBLIC)
                .createdAt(LocalDateTime.now())
                .build();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(e));
        when(storageService.generatePresignedDownloadUrl(anyString(), anyString())).thenReturn("u");
        when(metadataRepository.findMapByFileId(fileId)).thenReturn(Map.of());

        var resp = fileService.getFile(fileId, userId);
        assertThat(resp.getFileId()).isEqualTo(fileId);
    }

    @Test
    void getFile_sharedAccess_success() {
        UUID fileId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        FileEntity e = FileEntity.builder()
                .id(fileId)
                .fileName("f.txt")
                .originalName("f.txt")
                .fileSize(100L)
                .fileType("code")
                .mimeType("text/plain")
                .bucketName("b")
                .storagePath("p")
                .uploadedBy(otherUser)
                .accessLevel(FileEntity.AccessLevel.SHARED)
                .createdAt(LocalDateTime.now())
                .build();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(e));
        when(accessRepository.hasAnyAccess(fileId, userId)).thenReturn(true);
        when(storageService.generatePresignedDownloadUrl(anyString(), anyString())).thenReturn("u");
        when(metadataRepository.findMapByFileId(fileId)).thenReturn(Map.of());

        var resp = fileService.getFile(fileId, userId);
        assertThat(resp.getFileId()).isEqualTo(fileId);
    }

    @Test
    void uploadFile_avatarType_usesDataBucket() throws Exception {
        MockMultipartFile mp = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(uploadProperties.getMaxSizeAvatarBytes()).thenReturn(5L * 1024 * 1024);
        when(storageService.generateStoragePath(any(), anyString(), anyString())).thenReturn("entities/by-id/abc/media/avatar.jpg");
        when(storageService.generatePresignedDownloadUrl(anyString(), anyString())).thenReturn("http://url");
        when(fileRepository.insert(any())).thenAnswer(inv -> {
            FileEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(LocalDateTime.now());
            return e;
        });

        fileService.uploadFile(mp, userId, "avatar", FileEntity.AccessLevel.PRIVATE, null);
        verify(storageService).uploadFile(eq("file-service-dev-data"), anyString(), any(), anyString(), anyLong());
    }

    @Test
    void uploadFile_fileTooLarge_throws() {
        when(uploadProperties.getMaxSizeChatAttachmentBytes()).thenReturn(5L);
        MockMultipartFile mp = new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});

        assertThrows(FileValidationException.class, () ->
                fileService.uploadFile(mp, userId, "image", FileEntity.AccessLevel.PRIVATE, null));
    }

    @Test
    void uploadFile_nullAccessLevel_defaultsToPrivate() throws Exception {
        MockMultipartFile mp = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        when(storageService.generateStoragePath(any(), anyString(), anyString())).thenReturn("entities/by-id/abc/attachments/test.txt");
        when(storageService.generatePresignedDownloadUrl(anyString(), anyString())).thenReturn("http://url");
        when(fileRepository.insert(any())).thenAnswer(inv -> {
            FileEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(LocalDateTime.now());
            return e;
        });

        FileResponse resp = fileService.uploadFile(mp, userId, "code", null, null);
        assertThat(resp).isNotNull();
    }

    @Test
    void getFile_fileNotFound_throws() {
        UUID fileId = UUID.randomUUID();
        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());
        assertThrows(com.sthree.file.exception.FileNotFoundException.class, () -> fileService.getFile(fileId, userId));
    }

    @Test
    void deleteFile_fileNotFound_throws() {
        UUID fileId = UUID.randomUUID();
        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());
        assertThrows(com.sthree.file.exception.FileNotFoundException.class, () -> fileService.deleteFile(fileId, userId));
    }

    @Test
    void createPresignedUpload_success() {
        PresignedUploadRequest req = PresignedUploadRequest.builder()
                .originalName("test.txt")
                .contentType("text/plain")
                .fileSize(100L)
                .type("code")
                .accessLevel("private")
                .build();

        when(storageService.generateStoragePath(any(), anyString(), anyString())).thenReturn("entities/by-id/x/attachments/test.txt");
        when(storageService.generatePresignedUploadUrl(anyString(), anyString(), anyString())).thenReturn("http://upload-url");
        when(storageService.getUploadUrlExpiration()).thenReturn(LocalDateTime.now().plusHours(1));

        PresignedUploadResponse resp = fileService.createPresignedUpload(userId, req);
        assertThat(resp.getUploadUrl()).isEqualTo("http://upload-url");
        assertThat(resp.getStoragePath()).isEqualTo("entities/by-id/x/attachments/test.txt");
        assertThat(resp.getBucketName()).isEqualTo("file-service-dev-data");
    }

    @Test
    void confirmPresignedUpload_success() {
        ConfirmUploadRequest req = ConfirmUploadRequest.builder()
                .originalName("test.txt")
                .storagePath("users/x/test.txt")
                .bucketName("file-service-chat-attachments")
                .contentType("text/plain")
                .fileSize(100L)
                .type("code")
                .accessLevel("private")
                .checksum("abc123")
                .build();

        when(storageService.fileExists("file-service-chat-attachments", "users/x/test.txt")).thenReturn(true);
        when(fileRepository.insert(any())).thenAnswer(inv -> {
            FileEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(LocalDateTime.now());
            return e;
        });
        when(storageService.generatePresignedDownloadUrl(anyString(), anyString())).thenReturn("http://download-url");
        when(storageService.getDownloadUrlExpiration()).thenReturn(LocalDateTime.now().plusHours(1));

        FileResponse resp = fileService.confirmPresignedUpload(userId, req);
        assertThat(resp.getUrl()).isEqualTo("http://download-url");
    }

    @Test
    void confirmPresignedUpload_withMetadata() {
        ConfirmUploadRequest req = ConfirmUploadRequest.builder()
                .originalName("test.txt")
                .storagePath("users/x/test.txt")
                .bucketName("file-service-chat-attachments")
                .contentType("text/plain")
                .fileSize(100L)
                .type("code")
                .metadata(Map.of("key", "value"))
                .build();

        when(storageService.fileExists(anyString(), anyString())).thenReturn(true);
        when(fileRepository.insert(any())).thenAnswer(inv -> {
            FileEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(LocalDateTime.now());
            return e;
        });
        when(storageService.generatePresignedDownloadUrl(anyString(), anyString())).thenReturn("url");
        when(storageService.getDownloadUrlExpiration()).thenReturn(LocalDateTime.now().plusHours(1));

        FileResponse resp = fileService.confirmPresignedUpload(userId, req);
        assertThat(resp).isNotNull();
        verify(metadataRepository).insertBatch(any(), eq(Map.of("key", "value")));
    }

    @Test
    void confirmPresignedUpload_fileNotInStorage_throwsStorageException() {
        ConfirmUploadRequest req = ConfirmUploadRequest.builder()
                .originalName("test.txt")
                .storagePath("users/x/test.txt")
                .bucketName("file-service-chat-attachments")
                .contentType("text/plain")
                .fileSize(100L)
                .type("code")
                .build();

        when(storageService.fileExists("file-service-chat-attachments", "users/x/test.txt")).thenReturn(false);

        assertThrows(com.sthree.file.exception.StorageException.class, () ->
                fileService.confirmPresignedUpload(userId, req));
    }
}
