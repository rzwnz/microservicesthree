package com.sthree.file.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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
import com.sthree.file.service.FileShareService;
import com.sthree.file.service.StorageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileShareServiceTest {

    @Mock FileRepository fileRepository;
    @Mock FileShareRepository shareRepository;
    @Mock StorageService storageService;
    @Mock GarageProperties garageProperties;
    @Mock FileEventPublisher eventPublisher;
    @Spy PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks FileShareService service;

    private UUID userId;
    private UUID fileId;
    private FileEntity file;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        fileId = UUID.randomUUID();
        file = FileEntity.builder()
                .id(fileId)
                .fileName("test.txt")
                .originalName("test.txt")
                .uploadedBy(userId)
                .accessLevel(FileEntity.AccessLevel.PRIVATE)
                .bucketName("bucket")
                .storagePath("path/test.txt")
                .build();
    }

    @Test
    void createShare_success() {
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(shareRepository.insert(any())).thenAnswer(inv -> {
            FileShareEntity s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        FileShareRequest request = FileShareRequest.builder()
                .expiresAt(LocalDateTime.now().plusDays(7))
                .maxDownloads(10)
                .build();

        FileShareResponse response = service.createShare(fileId, userId, request);

        assertThat(response.getShareToken()).isNotBlank();
        assertThat(response.getShareUrl()).contains("/api/files/share/");
        assertThat(response.getMaxDownloads()).isEqualTo(10);
        assertThat(response.getPasswordProtected()).isFalse();

        verify(shareRepository).insert(any());
        verify(eventPublisher).publishFileShared(eq(fileId), eq(userId), any());
    }

    @Test
    void createShare_withPassword() {
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(shareRepository.insert(any())).thenAnswer(inv -> {
            FileShareEntity s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        FileShareRequest request = FileShareRequest.builder()
                .password("secret123")
                .build();

        FileShareResponse response = service.createShare(fileId, userId, request);
        assertThat(response.getPasswordProtected()).isTrue();
    }

    @Test
    void createShare_notOwner_throws() {
        UUID otherUser = UUID.randomUUID();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        FileShareRequest request = FileShareRequest.builder().build();
        assertThatThrownBy(() -> service.createShare(fileId, otherUser, request))
                .isInstanceOf(FileAccessDeniedException.class);
    }

    @Test
    void createShare_fileNotFound_throws() {
        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        FileShareRequest request = FileShareRequest.builder().build();
        assertThatThrownBy(() -> service.createShare(fileId, userId, request))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void accessSharedFile_success_noPassword() {
        String token = "abc123";
        FileShareEntity share = FileShareEntity.builder()
                .fileId(fileId)
                .shareToken(token)
                .sharedBy(userId)
                .downloadCount(0)
                .build();

        when(shareRepository.findByToken(token)).thenReturn(Optional.of(share));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(storageService.generatePresignedDownloadUrl(anyString(), anyString())).thenReturn("http://download");

        String url = service.accessSharedFile(token, null);

        assertThat(url).isEqualTo("http://download");
        verify(shareRepository).incrementDownloadCount(token);
    }

    @Test
    void accessSharedFile_expired_throws() {
        String token = "exp";
        FileShareEntity share = FileShareEntity.builder()
                .shareToken(token)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .downloadCount(0)
                .build();

        when(shareRepository.findByToken(token)).thenReturn(Optional.of(share));

        assertThatThrownBy(() -> service.accessSharedFile(token, null))
                .isInstanceOf(FileShareException.class);
    }

    @Test
    void accessSharedFile_limitReached_throws() {
        String token = "limit";
        FileShareEntity share = FileShareEntity.builder()
                .shareToken(token)
                .maxDownloads(5)
                .downloadCount(5)
                .build();

        when(shareRepository.findByToken(token)).thenReturn(Optional.of(share));

        assertThatThrownBy(() -> service.accessSharedFile(token, null))
                .isInstanceOf(FileShareException.class);
    }

    @Test
    void accessSharedFile_wrongPassword_throws() {
        String token = "pwd";
        FileShareEntity share = FileShareEntity.builder()
                .shareToken(token)
                .passwordHash("$2a$10$dummyhash")
                .downloadCount(0)
                .build();

        when(shareRepository.findByToken(token)).thenReturn(Optional.of(share));

        assertThatThrownBy(() -> service.accessSharedFile(token, "wrong"))
                .isInstanceOf(FileShareException.class);
    }

    @Test
    void accessSharedFile_invalidToken_throws() {
        when(shareRepository.findByToken("bogus")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.accessSharedFile("bogus", null))
                .isInstanceOf(FileShareException.class);
    }

    @Test
    void getSharedFileInfo_success() {
        String token = "info";
        FileShareEntity share = FileShareEntity.builder()
                .shareToken(token)
                .fileId(fileId)
                .downloadCount(0)
                .build();

        when(shareRepository.findByToken(token)).thenReturn(Optional.of(share));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        FileEntity result = service.getSharedFileInfo(token);
        assertThat(result.getId()).isEqualTo(fileId);
    }

    @Test
    void revokeShare_success() {
        String token = "revoke";
        FileShareEntity share = FileShareEntity.builder()
                .shareToken(token)
                .sharedBy(userId)
                .build();

        when(shareRepository.findByToken(token)).thenReturn(Optional.of(share));

        boolean result = service.revokeShare(token, userId);
        assertThat(result).isTrue();
        verify(shareRepository).deleteByToken(token);
    }

    @Test
    void revokeShare_notCreator_throws() {
        String token = "revoke";
        FileShareEntity share = FileShareEntity.builder()
                .shareToken(token)
                .sharedBy(UUID.randomUUID())
                .build();

        when(shareRepository.findByToken(token)).thenReturn(Optional.of(share));

        assertThatThrownBy(() -> service.revokeShare(token, userId))
                .isInstanceOf(FileAccessDeniedException.class);
    }

    @Test
    void getFileShares_returnsShares() {
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        FileShareEntity share = FileShareEntity.builder()
                .shareToken("tok1")
                .sharedBy(userId)
                .build();
        when(shareRepository.findByFileId(fileId)).thenReturn(List.of(share));

        List<FileShareResponse> result = service.getFileShares(fileId, userId);
        assertThat(result).hasSize(1);
    }

    @Test
    void getUserShares_returnsShares() {
        FileShareEntity share = FileShareEntity.builder()
                .shareToken("tok1")
                .sharedBy(userId)
                .build();
        when(shareRepository.findBySharedBy(userId)).thenReturn(List.of(share));

        List<FileShareResponse> result = service.getUserShares(userId);
        assertThat(result).hasSize(1);
    }

    @Test
    void isShareValid_validToken() {
        FileShareEntity share = FileShareEntity.builder()
                .shareToken("valid")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .maxDownloads(10)
                .downloadCount(0)
                .build();
        when(shareRepository.findByToken("valid")).thenReturn(Optional.of(share));

        assertThat(service.isShareValid("valid")).isTrue();
    }

    @Test
    void isShareValid_invalidToken() {
        when(shareRepository.findByToken("nope")).thenReturn(Optional.empty());
        assertThat(service.isShareValid("nope")).isFalse();
    }
}
