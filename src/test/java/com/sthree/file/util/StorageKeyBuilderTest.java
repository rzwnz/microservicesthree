package com.sthree.file.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StorageKeyBuilderTest {

    // ── entityObject ───────────────────────────────────────────

    @Test
    void entityObject_generatesCorrectPrefix() {
        UUID entityId = UUID.randomUUID();
        String key = StorageKeyBuilder.entityObject(entityId, "media", "photo.jpg");

        assertThat(key).startsWith("entities/by-id/" + entityId + "/media/");
        assertThat(key).endsWith(".jpg");
    }

    @Test
    void entityObject_noExtension() {
        UUID entityId = UUID.randomUUID();
        String key = StorageKeyBuilder.entityObject(entityId, "files", "readme");

        assertThat(key).startsWith("entities/by-id/" + entityId + "/files/");
        assertThat(key).doesNotContain(".");
    }

    @Test
    void entityObject_nullFilename_emptyExtension() {
        UUID entityId = UUID.randomUUID();
        String key = StorageKeyBuilder.entityObject(entityId, "files", null);

        assertThat(key).startsWith("entities/by-id/" + entityId + "/files/");
    }

    @Test
    void entityObject_withSpecificObjectId() {
        UUID entityId = UUID.randomUUID();
        UUID objectId = UUID.randomUUID();
        String key = StorageKeyBuilder.entityObject(entityId, "attachments", objectId, "doc.pdf");

        assertThat(key).isEqualTo("entities/by-id/" + entityId + "/attachments/" + objectId + ".pdf");
    }

    // ── convenience methods ────────────────────────────────────

    @Test
    void entityMedia_usesMediaCategory() {
        UUID entityId = UUID.randomUUID();
        String key = StorageKeyBuilder.entityMedia(entityId, "avatar.png");

        assertThat(key).contains("/media/");
        assertThat(key).endsWith(".png");
    }

    @Test
    void entityAttachment_usesAttachmentsCategory() {
        UUID entityId = UUID.randomUUID();
        String key = StorageKeyBuilder.entityAttachment(entityId, "report.pdf");

        assertThat(key).contains("/attachments/");
        assertThat(key).endsWith(".pdf");
    }

    @Test
    void entityFile_usesFilesCategory() {
        UUID entityId = UUID.randomUUID();
        String key = StorageKeyBuilder.entityFile(entityId, "data.csv");

        assertThat(key).contains("/files/");
        assertThat(key).endsWith(".csv");
    }

    // ── artifacts ──────────────────────────────────────────────

    @Test
    void artifactById_generatesCorrectPrefix() {
        UUID artifactId = UUID.randomUUID();
        String key = StorageKeyBuilder.artifactById(artifactId, "export.csv");

        assertThat(key).startsWith("artifacts/by-id/" + artifactId + "/files/");
        assertThat(key).endsWith(".csv");
    }

    @Test
    void artifactByDate_generatesDatePath() {
        UUID artifactId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 3, 2);
        String key = StorageKeyBuilder.artifactByDate(date, artifactId, "reports", "summary.json");

        assertThat(key).isEqualTo("artifacts/by-date/2026/03/02/reports/" + artifactId + ".json");
    }

    // ── system ─────────────────────────────────────────────────

    @Test
    void systemBackup_generatesCorrectPath() {
        LocalDate date = LocalDate.of(2026, 1, 15);
        String key = StorageKeyBuilder.systemBackup(date, "db-full.tar.gz");

        assertThat(key).isEqualTo("system/backups/2026/01/15/db-full.tar.gz");
    }

    @Test
    void systemSnapshot_generatesCorrectPrefix() {
        UUID snapshotId = UUID.randomUUID();
        String key = StorageKeyBuilder.systemSnapshot(snapshotId, "data.bin");

        assertThat(key).startsWith("system/snapshots/" + snapshotId + "/");
        assertThat(key).endsWith(".bin");
    }

    // ── tmp ────────────────────────────────────────────────────

    @Test
    void tempUpload_generatesCorrectPath() {
        UUID uploadId = UUID.randomUUID();
        String key = StorageKeyBuilder.tempUpload(uploadId, "file.txt");

        assertThat(key).isEqualTo("tmp/uploads/" + uploadId + "/file.txt");
    }

    @Test
    void tempProcessing_generatesCorrectPath() {
        UUID jobId = UUID.randomUUID();
        String key = StorageKeyBuilder.tempProcessing(jobId, "chunk.dat");

        assertThat(key).isEqualTo("tmp/processing/" + jobId + "/chunk.dat");
    }

    // ── helper methods ─────────────────────────────────────────

    @Test
    void extensionOf_withDot() {
        assertThat(StorageKeyBuilder.extensionOf("photo.JPG")).isEqualTo(".jpg");
        assertThat(StorageKeyBuilder.extensionOf("archive.tar.gz")).isEqualTo(".gz");
    }

    @Test
    void extensionOf_noDot() {
        assertThat(StorageKeyBuilder.extensionOf("readme")).isEmpty();
        assertThat(StorageKeyBuilder.extensionOf(null)).isEmpty();
        assertThat(StorageKeyBuilder.extensionOf("")).isEmpty();
    }

    @Test
    void sanitize_removesUnsafeChars() {
        assertThat(StorageKeyBuilder.sanitize("my file (1).txt")).isEqualTo("my_file__1_.txt");
        assertThat(StorageKeyBuilder.sanitize(null)).isEqualTo("unnamed");
    }

    @Test
    void eachCall_generatesUniqueObjectId() {
        UUID entityId = UUID.randomUUID();
        String key1 = StorageKeyBuilder.entityMedia(entityId, "a.png");
        String key2 = StorageKeyBuilder.entityMedia(entityId, "a.png");

        assertThat(key1).isNotEqualTo(key2);
    }
}
