package com.sthree.file.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sthree.file.entity.FileAccessEntity;
import com.sthree.file.entity.FileEntity;
import com.sthree.file.entity.FileMetadataEntity;
import com.sthree.file.entity.FileShareEntity;
import com.sthree.file.entity.FileThumbnailEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityTest {

    // --- FileEntity ---

    @Test
    void fileEntity_isOwner() {
        UUID userId = UUID.randomUUID();
        FileEntity entity = FileEntity.builder().uploadedBy(userId).build();

        assertThat(entity.isOwner(userId)).isTrue();
        assertThat(entity.isOwner(UUID.randomUUID())).isFalse();
        assertThat(entity.isOwner(null)).isFalse();
    }

    @Test
    void fileEntity_isOwner_nullUploadedBy() {
        FileEntity entity = FileEntity.builder().uploadedBy(null).build();
        assertThat(entity.isOwner(UUID.randomUUID())).isFalse();
    }

    @Test
    void fileEntity_isDeleted() {
        FileEntity notDeleted = FileEntity.builder().build();
        assertThat(notDeleted.isDeleted()).isFalse();

        FileEntity deleted = FileEntity.builder().deletedAt(LocalDateTime.now()).build();
        assertThat(deleted.isDeleted()).isTrue();
    }

    // --- FileEntity.AccessLevel enum ---

    @ParameterizedTest
    @CsvSource({"public,PUBLIC", "private,PRIVATE", "shared,SHARED"})
    void accessLevel_fromString(String value, String expected) {
        FileEntity.AccessLevel level = FileEntity.AccessLevel.fromString(value);
        assertThat(level.name()).isEqualTo(expected);
    }

    @Test
    void accessLevel_fromString_invalid() {
        assertThatThrownBy(() -> FileEntity.AccessLevel.fromString("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accessLevel_getValue() {
        assertThat(FileEntity.AccessLevel.PUBLIC.getValue()).isEqualTo("public");
        assertThat(FileEntity.AccessLevel.PRIVATE.getValue()).isEqualTo("private");
        assertThat(FileEntity.AccessLevel.SHARED.getValue()).isEqualTo("shared");
    }

    // --- FileEntity.FileType enum ---

    @ParameterizedTest
    @CsvSource({"image,IMAGE", "code,CODE", "document,DOCUMENT"})
    void fileType_fromString(String value, String expected) {
        FileEntity.FileType type = FileEntity.FileType.fromString(value);
        assertThat(type.name()).isEqualTo(expected);
    }

    @Test
    void fileType_fromString_invalid() {
        assertThatThrownBy(() -> FileEntity.FileType.fromString("bogus"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fileType_getValue() {
        assertThat(FileEntity.FileType.IMAGE.getValue()).isEqualTo("image");
        assertThat(FileEntity.FileType.CODE.getValue()).isEqualTo("code");
        assertThat(FileEntity.FileType.DOCUMENT.getValue()).isEqualTo("document");
    }

    // --- FileAccessEntity ---

    @Test
    void fileAccessEntity_isExpired() {
        FileAccessEntity expired = FileAccessEntity.builder()
                .expiresAt(LocalDateTime.now().minusHours(1)).build();
        assertThat(expired.isExpired()).isTrue();
        assertThat(expired.isValid()).isFalse();

        FileAccessEntity notExpired = FileAccessEntity.builder()
                .expiresAt(LocalDateTime.now().plusHours(1)).build();
        assertThat(notExpired.isExpired()).isFalse();
        assertThat(notExpired.isValid()).isTrue();

        FileAccessEntity noExpiry = FileAccessEntity.builder().build();
        assertThat(noExpiry.isExpired()).isFalse();
        assertThat(noExpiry.isValid()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"read,READ", "write,WRITE", "delete,DELETE"})
    void accessType_fromString(String value, String expected) {
        FileAccessEntity.AccessType type = FileAccessEntity.AccessType.fromString(value);
        assertThat(type.name()).isEqualTo(expected);
    }

    @Test
    void accessType_fromString_invalid() {
        assertThatThrownBy(() -> FileAccessEntity.AccessType.fromString("admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readAccess_factory() {
        UUID fileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID grantedBy = UUID.randomUUID();
        FileAccessEntity access = FileAccessEntity.readAccess(fileId, userId, grantedBy);

        assertThat(access.getAccessType()).isEqualTo(FileAccessEntity.AccessType.READ);
        assertThat(access.getFileId()).isEqualTo(fileId);
    }

    @Test
    void writeAccess_factory() {
        FileAccessEntity access = FileAccessEntity.writeAccess(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        assertThat(access.getAccessType()).isEqualTo(FileAccessEntity.AccessType.WRITE);
    }

    // --- FileShareEntity ---

    @Test
    void fileShareEntity_isExpired() {
        FileShareEntity expired = FileShareEntity.builder()
                .expiresAt(LocalDateTime.now().minusHours(1)).build();
        assertThat(expired.isExpired()).isTrue();

        FileShareEntity notExpired = FileShareEntity.builder()
                .expiresAt(LocalDateTime.now().plusHours(1)).build();
        assertThat(notExpired.isExpired()).isFalse();

        FileShareEntity noExpiry = FileShareEntity.builder().build();
        assertThat(noExpiry.isExpired()).isFalse();
    }

    @Test
    void fileShareEntity_isDownloadLimitReached() {
        FileShareEntity reached = FileShareEntity.builder()
                .maxDownloads(5).downloadCount(5).build();
        assertThat(reached.isDownloadLimitReached()).isTrue();

        FileShareEntity notReached = FileShareEntity.builder()
                .maxDownloads(5).downloadCount(3).build();
        assertThat(notReached.isDownloadLimitReached()).isFalse();

        FileShareEntity noLimit = FileShareEntity.builder()
                .downloadCount(100).build();
        assertThat(noLimit.isDownloadLimitReached()).isFalse();
    }

    @Test
    void fileShareEntity_isValid() {
        FileShareEntity valid = FileShareEntity.builder()
                .expiresAt(LocalDateTime.now().plusDays(1))
                .maxDownloads(10).downloadCount(3).build();
        assertThat(valid.isValid()).isTrue();

        FileShareEntity invalid = FileShareEntity.builder()
                .expiresAt(LocalDateTime.now().minusDays(1)).build();
        assertThat(invalid.isValid()).isFalse();
    }

    @Test
    void fileShareEntity_isPasswordProtected() {
        FileShareEntity withPwd = FileShareEntity.builder()
                .passwordHash("hash").build();
        assertThat(withPwd.isPasswordProtected()).isTrue();

        FileShareEntity noPwd = FileShareEntity.builder().build();
        assertThat(noPwd.isPasswordProtected()).isFalse();

        FileShareEntity emptyPwd = FileShareEntity.builder()
                .passwordHash("").build();
        assertThat(emptyPwd.isPasswordProtected()).isFalse();
    }

    @Test
    void fileShareEntity_incrementDownloadCount() {
        FileShareEntity entity = FileShareEntity.builder().downloadCount(3).build();
        entity.incrementDownloadCount();
        assertThat(entity.getDownloadCount()).isEqualTo(4);

        FileShareEntity fresh = FileShareEntity.builder().build();
        fresh.incrementDownloadCount();
        assertThat(fresh.getDownloadCount()).isEqualTo(1);
    }

    @Test
    void fileShareEntity_publicShare_factory() {
        UUID fileId = UUID.randomUUID();
        UUID sharedBy = UUID.randomUUID();
        FileShareEntity share = FileShareEntity.publicShare(fileId, sharedBy, "token123");

        assertThat(share.getFileId()).isEqualTo(fileId);
        assertThat(share.getShareToken()).isEqualTo("token123");
        assertThat(share.getAccessLevel()).isEqualTo(FileEntity.AccessLevel.PUBLIC);
        assertThat(share.getDownloadCount()).isEqualTo(0);
    }

    @Test
    void fileShareEntity_shareWithExpiration_factory() {
        UUID fileId = UUID.randomUUID();
        UUID sharedBy = UUID.randomUUID();
        LocalDateTime exp = LocalDateTime.now().plusDays(7);
        FileShareEntity share = FileShareEntity.shareWithExpiration(fileId, sharedBy, "token", exp, 10);

        assertThat(share.getExpiresAt()).isEqualTo(exp);
        assertThat(share.getMaxDownloads()).isEqualTo(10);
    }

    // --- FileMetadataEntity ---

    @Test
    void fileMetadataEntity_of_factory() {
        UUID fileId = UUID.randomUUID();
        FileMetadataEntity meta = FileMetadataEntity.of(fileId, "width", "1920");

        assertThat(meta.getFileId()).isEqualTo(fileId);
        assertThat(meta.getMetadataKey()).isEqualTo("width");
        assertThat(meta.getMetadataValue()).isEqualTo("1920");
        assertThat(meta.getId()).isNotNull();
        assertThat(meta.getCreatedAt()).isNotNull();
    }

    @Test
    void fileMetadataEntity_metadataKeysExist() {
        // Validate the constants exist and are sensible
        assertThat(FileMetadataEntity.MetadataKey.WIDTH).isEqualTo("width");
        assertThat(FileMetadataEntity.MetadataKey.HEIGHT).isEqualTo("height");
        assertThat(FileMetadataEntity.MetadataKey.DESCRIPTION).isEqualTo("description");
    }

    // --- FileThumbnailEntity ---

    @Test
    void fileThumbnailEntity_of_factory() {
        UUID fileId = UUID.randomUUID();
        FileThumbnailEntity thumb = FileThumbnailEntity.of(
                fileId, "path/thumb", FileThumbnailEntity.ThumbnailSize.MEDIUM, 300, 300);

        assertThat(thumb.getFileId()).isEqualTo(fileId);
        assertThat(thumb.getThumbnailPath()).isEqualTo("path/thumb");
        assertThat(thumb.getWidth()).isEqualTo(300);
    }

    @ParameterizedTest
    @CsvSource({"small,SMALL", "medium,MEDIUM", "large,LARGE"})
    void thumbnailSize_fromString(String value, String expected) {
        FileThumbnailEntity.ThumbnailSize size = FileThumbnailEntity.ThumbnailSize.fromString(value);
        assertThat(size.name()).isEqualTo(expected);
    }

    @Test
    void thumbnailSize_fromString_invalid() {
        assertThatThrownBy(() -> FileThumbnailEntity.ThumbnailSize.fromString("tiny"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
