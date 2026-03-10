package com.sthree.file.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.sthree.file.controller.FileController;
import com.sthree.file.dto.*;
import com.sthree.file.entity.FileEntity;
import com.sthree.file.exception.UnauthorizedException;
import com.sthree.file.service.FileService;
import com.sthree.file.service.FileShareService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    FileService fileService;

    @Mock
    FileShareService shareService;

    FileController controller;

    UUID userId;

    @BeforeEach
    void initController() {
        userId = UUID.randomUUID();
        controller = new FileController(fileService, shareService);
    }

    @Test
    void uploadFile_endpoint_returns200() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes());
        UUID id = UUID.randomUUID();
        FileResponse resp = FileResponse.builder().fileId(id).originalName("a.txt").url("u").build();

        Mockito.when(fileService.uploadFile(any(), any(), any(), any(), any())).thenReturn(resp);

        ResponseEntity<?> r = controller.uploadFile(file, "code", "private", userId);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        ApiResponse body = (ApiResponse) r.getBody();
        assertThat(body.getData()).isNotNull();
    }

    @Test
    void getFile_endpoint_returns200() {
        UUID fileId = UUID.randomUUID();
        FileResponse resp = FileResponse.builder().fileId(fileId).originalName("a.txt").build();
        when(fileService.getFile(eq(fileId), any())).thenReturn(resp);

        ResponseEntity<?> r = controller.getFile(fileId, userId);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        ApiResponse body = (ApiResponse) r.getBody();
        assertThat(body.getData()).isNotNull();
    }

    @Test
    void uploadFile_withoutUserId_throwsUnauthorized() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> controller.uploadFile(file, "code", "private", null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void downloadFile_returns200_withCorrectHeaders() {
        UUID fileId = UUID.randomUUID();
        FileResponse fileResp = FileResponse.builder()
                .fileId(fileId)
                .originalName("test.pdf")
                .mimeType("application/pdf")
                .fileSize(1234L)
                .build();

        InputStream mockStream = new ByteArrayInputStream("file-data".getBytes());
        when(fileService.getFile(eq(fileId), eq(userId))).thenReturn(fileResp);
        when(fileService.getFileStream(eq(fileId), eq(userId))).thenReturn(mockStream);

        ResponseEntity<Resource> r = controller.downloadFile(fileId, userId);
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getHeaders().getContentType().toString()).isEqualTo("application/pdf");
        assertThat(r.getHeaders().getContentLength()).isEqualTo(1234L);
        assertThat(r.getHeaders().getContentDisposition().toString()).contains("test.pdf");
    }

    @Test
    void downloadFile_withoutUserId_throwsUnauthorized() {
        assertThatThrownBy(() -> controller.downloadFile(UUID.randomUUID(), null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deleteFile_returns200() {
        UUID fileId = UUID.randomUUID();
        when(fileService.deleteFile(eq(fileId), eq(userId))).thenReturn(true);

        ResponseEntity<?> r = controller.deleteFile(fileId, userId);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        ApiResponse<Map<String, Boolean>> body = (ApiResponse<Map<String, Boolean>>) r.getBody();
        assertThat(body.getMessage()).isEqualTo("File deleted successfully");
    }

    @Test
    void deleteFile_withoutUserId_throwsUnauthorized() {
        assertThatThrownBy(() -> controller.deleteFile(UUID.randomUUID(), null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getFileMetadata_returns200() {
        UUID fileId = UUID.randomUUID();
        FileMetadataResponse metaResp = Mockito.mock(FileMetadataResponse.class);

        when(fileService.getFileMetadata(eq(fileId), eq(userId))).thenReturn(metaResp);

        ResponseEntity<?> r = controller.getFileMetadata(fileId, userId);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        ApiResponse<FileMetadataResponse> body = (ApiResponse<FileMetadataResponse>) r.getBody();
        assertThat(body.getData()).isNotNull();
    }

    @Test
    void getFileMetadata_withoutUserId_throwsUnauthorized() {
        assertThatThrownBy(() -> controller.getFileMetadata(UUID.randomUUID(), null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shareFile_returns200() {
        UUID fileId = UUID.randomUUID();
        FileShareRequest request = FileShareRequest.builder().build();
        FileShareResponse shareResp = FileShareResponse.builder()
                .shareToken("abc123")
                .build();

        when(shareService.createShare(eq(fileId), eq(userId), any())).thenReturn(shareResp);

        ResponseEntity<?> r = controller.shareFile(fileId, request, userId);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        ApiResponse body = (ApiResponse) r.getBody();
        assertThat(body.getMessage()).isEqualTo("File shared successfully");
    }

    @Test
    void shareFile_withoutUserId_throwsUnauthorized() {
        assertThatThrownBy(() -> controller.shareFile(UUID.randomUUID(), FileShareRequest.builder().build(), null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getSharedFile_returns200() {
        UUID fileId = UUID.randomUUID();
        FileEntity fileEntity = FileEntity.builder()
                .id(fileId)
                .fileName("f.txt")
                .originalName("original.txt")
                .fileSize(500L)
                .fileType("document")
                .mimeType("text/plain")
                .accessLevel(FileEntity.AccessLevel.PUBLIC)
                .build();

        when(shareService.getSharedFileInfo("token123")).thenReturn(fileEntity);
        when(shareService.accessSharedFile("token123", null)).thenReturn("http://download-url");

        ResponseEntity<?> r = controller.getSharedFile("token123", null);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void getSharedFile_withPassword() {
        UUID fileId = UUID.randomUUID();
        FileEntity fileEntity = FileEntity.builder()
                .id(fileId)
                .fileName("secret.txt")
                .originalName("secret.txt")
                .fileSize(200L)
                .fileType("document")
                .mimeType("text/plain")
                .accessLevel(null)
                .build();

        when(shareService.getSharedFileInfo("token456")).thenReturn(fileEntity);
        when(shareService.accessSharedFile("token456", "pass")).thenReturn("http://download-url");

        ResponseEntity<?> r = controller.getSharedFile("token456", "pass");
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void downloadSharedFile_returns302Redirect() {
        FileEntity fileEntity = FileEntity.builder()
                .id(UUID.randomUUID())
                .fileName("f.txt")
                .originalName("f.txt")
                .fileSize(100L)
                .build();

        when(shareService.getSharedFileInfo("tok")).thenReturn(fileEntity);
        when(shareService.accessSharedFile("tok", null)).thenReturn("http://presigned-url");

        ResponseEntity<Resource> r = controller.downloadSharedFile("tok", null);
        assertThat(r.getStatusCode().value()).isEqualTo(302);
        assertThat(r.getHeaders().getLocation().toString()).isEqualTo("http://presigned-url");
    }

    @Test
    void revokeShare_returns200() {
        when(shareService.revokeShare("tok", userId)).thenReturn(true);

        ResponseEntity<?> r = controller.revokeShare("tok", userId);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        ApiResponse body = (ApiResponse) r.getBody();
        assertThat(body.getMessage()).isEqualTo("Share revoked successfully");
    }

    @Test
    void revokeShare_withoutUserId_throwsUnauthorized() {
        assertThatThrownBy(() -> controller.revokeShare("tok", null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void createPresignedUpload_endpoint_returns200() {
        PresignedUploadRequest request = PresignedUploadRequest.builder()
                .originalName("a.txt")
                .contentType("text/plain")
                .fileSize(5L)
                .type("code")
                .build();
        PresignedUploadResponse response = PresignedUploadResponse.builder()
                .uploadUrl("http://example/upload")
                .storagePath("users/x/1.txt")
            .bucketName("file-service-chat-attachments")
                .build();

        when(fileService.createPresignedUpload(eq(userId), any())).thenReturn(response);

        ResponseEntity<?> r = controller.createPresignedUpload(request, userId);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        ApiResponse body = (ApiResponse) r.getBody();
        assertThat(body.getData()).isNotNull();
    }

    @Test
    void confirmUpload_endpoint_returns200() {
        ConfirmUploadRequest request = ConfirmUploadRequest.builder()
                .originalName("a.txt")
                .storagePath("users/x/1.txt")
            .bucketName("file-service-chat-attachments")
                .contentType("text/plain")
                .fileSize(5L)
                .type("code")
                .accessLevel("private")
                .build();
        FileResponse response = FileResponse.builder()
                .fileId(UUID.randomUUID())
                .originalName("a.txt")
                .url("http://example/download")
                .build();

        when(fileService.confirmPresignedUpload(eq(userId), any())).thenReturn(response);

        ResponseEntity<?> r = controller.confirmUpload(request, userId);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void presignedEndpoints_withoutUserId_throwUnauthorized() {
        PresignedUploadRequest presignedRequest = PresignedUploadRequest.builder()
                .originalName("a.txt")
                .contentType("text/plain")
                .fileSize(5L)
                .type("code")
                .build();

        ConfirmUploadRequest confirmRequest = ConfirmUploadRequest.builder()
                .originalName("a.txt")
                .storagePath("users/x/1.txt")
            .bucketName("file-service-chat-attachments")
                .contentType("text/plain")
                .fileSize(5L)
                .type("code")
                .build();

        assertThatThrownBy(() -> controller.createPresignedUpload(presignedRequest, null))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> controller.confirmUpload(confirmRequest, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getUserFiles_returns200() {
        List<FileResponse> files = List.of(
                FileResponse.builder().fileId(UUID.randomUUID()).originalName("a.txt").build()
        );
        when(fileService.getUserFiles(userId)).thenReturn(files);

        ResponseEntity<?> r = controller.getUserFiles(userId, userId);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void getUserFiles_otherUser_throwsForbidden() {
        UUID otherUserId = UUID.randomUUID();
        assertThatThrownBy(() -> controller.getUserFiles(otherUserId, userId))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getUserFiles_withoutAuth_throwsUnauthorized() {
        assertThatThrownBy(() -> controller.getUserFiles(userId, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getStorageUsage_returns200() {
        FileService.StorageUsage usage = new FileService.StorageUsage(12345L, 1073741824L, 5L);
        when(fileService.getStorageUsage(userId)).thenReturn(usage);

        ResponseEntity<?> r = controller.getStorageUsage(userId, userId);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        ApiResponse body = (ApiResponse) r.getBody();
        assertThat(body.getData()).isNotNull();
    }

    @Test
    void getStorageUsage_otherUser_throwsForbidden() {
        UUID otherUserId = UUID.randomUUID();
        assertThatThrownBy(() -> controller.getStorageUsage(otherUserId, userId))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getStorageUsage_withoutAuth_throwsUnauthorized() {
        assertThatThrownBy(() -> controller.getStorageUsage(userId, null))
                .isInstanceOf(UnauthorizedException.class);
    }
}
