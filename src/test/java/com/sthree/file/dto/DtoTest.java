package com.sthree.file.dto;

import org.junit.jupiter.api.Test;

import com.sthree.file.dto.ApiResponse;
import com.sthree.file.dto.ConfirmUploadRequest;
import com.sthree.file.dto.FileMetadataResponse;
import com.sthree.file.dto.FileResponse;
import com.sthree.file.dto.FileShareRequest;
import com.sthree.file.dto.FileShareResponse;
import com.sthree.file.dto.PresignedUploadRequest;
import com.sthree.file.dto.PresignedUploadResponse;
import com.sthree.file.entity.FileEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DtoTest {

    // --- ApiResponse ---

    @Test
    void apiResponse_success_withData() {
        ApiResponse<Integer> response = ApiResponse.success(42);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(42);
    }

    @Test
    void apiResponse_success_messageOnly() {
        ApiResponse<Void> response = ApiResponse.success("done");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("done");
    }

    @Test
    void apiResponse_successWithMessageAndData() {
        ApiResponse<Integer> response = ApiResponse.success("custom msg", 99);
        assertThat(response.getMessage()).isEqualTo("custom msg");
        assertThat(response.getData()).isEqualTo(99);
    }

    @Test
    void apiResponse_error() {
        ApiResponse<Void> response = ApiResponse.error("failed", "ERR_CODE");
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("failed");
        assertThat(response.getErrorCode()).isEqualTo("ERR_CODE");
    }

    @Test
    void apiResponse_errorMessageOnly() {
        ApiResponse<Void> response = ApiResponse.error("oops");
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("oops");
    }

    // --- FileResponse ---

    @Test
    void fileResponse_fromEntity() {
        UUID id = UUID.randomUUID();
        UUID uploadedBy = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        FileEntity entity = FileEntity.builder()
                .id(id)
                .fileName("file.txt")
                .originalName("original.txt")
                .fileSize(1024L)
                .fileType("code")
                .mimeType("text/plain")
                .accessLevel(FileEntity.AccessLevel.PRIVATE)
                .createdAt(now)
                .uploadedBy(uploadedBy)
                .build();

        FileResponse response = FileResponse.fromEntity(entity);

        assertThat(response.getFileId()).isEqualTo(id);
        assertThat(response.getOriginalName()).isEqualTo("original.txt");
        assertThat(response.getAccessLevel()).isEqualTo("private");
        assertThat(response.getUploadedBy()).isEqualTo(uploadedBy);
    }

    @Test
    void fileResponse_fromEntity_nullAccessLevel() {
        FileEntity entity = FileEntity.builder()
                .id(UUID.randomUUID())
                .build();

        FileResponse response = FileResponse.fromEntity(entity);
        assertThat(response.getAccessLevel()).isNull();
    }

    @Test
    void fileResponse_fromEntityWithUrl() {
        FileEntity entity = FileEntity.builder()
                .id(UUID.randomUUID())
                .accessLevel(FileEntity.AccessLevel.PUBLIC)
                .build();

        FileResponse response = FileResponse.fromEntityWithUrl(entity, "http://dl", "http://thumb");
        assertThat(response.getUrl()).isEqualTo("http://dl");
        assertThat(response.getThumbnailUrl()).isEqualTo("http://thumb");
    }

    // --- PresignedUploadRequest ---

    @Test
    void presignedUploadRequest_builder() {
        PresignedUploadRequest request = PresignedUploadRequest.builder()
                .originalName("file.txt")
                .type("code")
                .contentType("text/plain")
                .fileSize(1024L)
                .accessLevel("private")
                .build();

        assertThat(request.getOriginalName()).isEqualTo("file.txt");
        assertThat(request.getFileSize()).isEqualTo(1024L);
    }

    // --- PresignedUploadResponse ---

    @Test
    void presignedUploadResponse_builder() {
        LocalDateTime exp = LocalDateTime.now().plusMinutes(30);
        PresignedUploadResponse response = PresignedUploadResponse.builder()
                .uploadUrl("http://upload")
                .storagePath("users/x/f.txt")
                .bucketName("bucket")
                .expiresAt(exp)
                .build();

        assertThat(response.getUploadUrl()).isEqualTo("http://upload");
        assertThat(response.getExpiresAt()).isEqualTo(exp);
    }

    // --- ConfirmUploadRequest ---

    @Test
    void confirmUploadRequest_builder() {
        ConfirmUploadRequest request = ConfirmUploadRequest.builder()
                .storagePath("path")
                .bucketName("bucket")
                .originalName("file.txt")
                .type("code")
                .contentType("text/plain")
                .fileSize(100L)
                .accessLevel("private")
                .checksum("sha256hash")
                .metadata(Map.of("key", "value"))
                .build();

        assertThat(request.getStoragePath()).isEqualTo("path");
        assertThat(request.getMetadata()).containsEntry("key", "value");
    }

    // --- FileShareRequest ---

    @Test
    void fileShareRequest_builder() {
        LocalDateTime exp = LocalDateTime.now().plusDays(7);
        FileShareRequest request = FileShareRequest.builder()
                .password("secret")
                .expiresAt(exp)
                .maxDownloads(10)
                .accessLevel("public")
                .build();

        assertThat(request.getPassword()).isEqualTo("secret");
        assertThat(request.getMaxDownloads()).isEqualTo(10);
    }

    // --- FileShareResponse ---

    @Test
    void fileShareResponse_builder() {
        FileShareResponse response = FileShareResponse.builder()
                .shareToken("token123")
                .shareUrl("/api/files/share/token123")
                .passwordProtected(true)
                .maxDownloads(5)
                .build();

        assertThat(response.getShareToken()).isEqualTo("token123");
        assertThat(response.getPasswordProtected()).isTrue();
    }

    // --- FileMetadataResponse ---

    @Test
    void fileMetadataResponse_builder() {
        FileMetadataResponse.FileMetadataDetails details = FileMetadataResponse.FileMetadataDetails.builder()
                .fileName("test.txt")
                .fileSize(1024L)
                .fileType("document")
                .mimeType("text/plain")
                .dimensions(FileMetadataResponse.Dimensions.builder().width(100).height(200).build())
                .build();

        FileMetadataResponse response = FileMetadataResponse.builder()
                .fileId(UUID.randomUUID())
                .metadata(details)
                .build();

        assertThat(response.getMetadata().getFileName()).isEqualTo("test.txt");
        assertThat(response.getMetadata().getDimensions().getWidth()).isEqualTo(100);
    }
}
