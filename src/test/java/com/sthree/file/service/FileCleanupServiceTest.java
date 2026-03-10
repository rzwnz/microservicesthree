package com.sthree.file.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sthree.file.entity.FileEntity;
import com.sthree.file.entity.FileThumbnailEntity;
import com.sthree.file.repository.*;
import com.sthree.file.service.FileCleanupService;
import com.sthree.file.service.StorageService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileCleanupServiceTest {

    @Mock FileRepository fileRepository;
    @Mock FileMetadataRepository metadataRepository;
    @Mock FileAccessRepository accessRepository;
    @Mock FileShareRepository shareRepository;
    @Mock FileThumbnailRepository thumbnailRepository;
    @Mock StorageService storageService;

    @InjectMocks FileCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cleanupService, "retentionDays", 30);
        ReflectionTestUtils.setField(cleanupService, "orphanedCleanupEnabled", true);
        ReflectionTestUtils.setField(cleanupService, "tempCleanupEnabled", true);
        ReflectionTestUtils.setField(cleanupService, "tempFileMaxAgeHours", 24);
    }

    @Test
    void cleanupSoftDeletedFiles_deletesExpiredFiles() {
        UUID fileId = UUID.randomUUID();
        FileEntity file = FileEntity.builder()
                .id(fileId)
                .bucketName("bucket")
                .storagePath("path/file.txt")
                .deletedAt(LocalDateTime.now().minusDays(31))
                .build();

        FileThumbnailEntity thumb = FileThumbnailEntity.builder()
                .id(UUID.randomUUID())
                .fileId(fileId)
                .thumbnailPath("path/file.txt/thumbnail_medium")
                .thumbnailSize(FileThumbnailEntity.ThumbnailSize.MEDIUM)
                .build();

        when(fileRepository.findFilesForCleanup(any())).thenReturn(List.of(file));
        when(thumbnailRepository.findByFileId(fileId)).thenReturn(List.of(thumb));

        cleanupService.cleanupSoftDeletedFiles();

        verify(storageService).deleteFile("bucket", "path/file.txt");
        verify(storageService).deleteFile("bucket", "path/file.txt/thumbnail_medium");
        verify(thumbnailRepository).deleteByFileId(fileId);
        verify(metadataRepository).deleteByFileId(fileId);
        verify(accessRepository).deleteByFileId(fileId);
        verify(shareRepository).deleteByFileId(fileId);
        verify(fileRepository).hardDelete(fileId);
    }

    @Test
    void cleanupSoftDeletedFiles_disabled_doesNothing() {
        ReflectionTestUtils.setField(cleanupService, "orphanedCleanupEnabled", false);

        cleanupService.cleanupSoftDeletedFiles();

        verifyNoInteractions(fileRepository, storageService);
    }

    @Test
    void cleanupSoftDeletedFiles_noFiles_doesNothing() {
        when(fileRepository.findFilesForCleanup(any())).thenReturn(Collections.emptyList());

        cleanupService.cleanupSoftDeletedFiles();

        verify(storageService, never()).deleteFile(anyString(), anyString());
    }

    @Test
    void cleanupSoftDeletedFiles_storageError_continuesWithNext() {
        UUID fileId1 = UUID.randomUUID();
        UUID fileId2 = UUID.randomUUID();
        FileEntity file1 = FileEntity.builder()
                .id(fileId1).bucketName("b").storagePath("p1").build();
        FileEntity file2 = FileEntity.builder()
                .id(fileId2).bucketName("b").storagePath("p2").build();

        when(fileRepository.findFilesForCleanup(any())).thenReturn(List.of(file1, file2));
        when(thumbnailRepository.findByFileId(any())).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("S3 error")).when(storageService).deleteFile("b", "p1");

        cleanupService.cleanupSoftDeletedFiles();

        // file2 still processed despite file1 failure
        verify(storageService).deleteFile("b", "p2");
        verify(fileRepository).hardDelete(fileId2);
    }

    @Test
    void cleanupExpiredShares() {
        when(shareRepository.cleanupExpired()).thenReturn(5);

        cleanupService.cleanupExpiredShares();

        verify(shareRepository).cleanupExpired();
    }

    @Test
    void cleanupExpiredAccess() {
        when(accessRepository.cleanupExpired()).thenReturn(3);

        cleanupService.cleanupExpiredAccess();

        verify(accessRepository).cleanupExpired();
    }
}
