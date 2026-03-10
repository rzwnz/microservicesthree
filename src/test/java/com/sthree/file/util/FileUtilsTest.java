package com.sthree.file.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sthree.file.util.FileUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FileUtilsTest {

    // --- getExtension ---

    @Test
    void getExtension_returnsExtension() {
        assertThat(FileUtils.getExtension("photo.JPG")).contains("jpg");
        assertThat(FileUtils.getExtension("archive.tar.gz")).contains("gz");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"noext", ".hidden", "trailing."})
    void getExtension_returnsEmpty(String input) {
        assertThat(FileUtils.getExtension(input)).isEmpty();
    }

    // --- getBaseName ---

    @Test
    void getBaseName_stripsExtension() {
        assertThat(FileUtils.getBaseName("file.txt")).isEqualTo("file");
        assertThat(FileUtils.getBaseName("archive.tar.gz")).isEqualTo("archive.tar");
    }

    @Test
    void getBaseName_noExtension_returnsAsIs() {
        assertThat(FileUtils.getBaseName("noext")).isEqualTo("noext");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void getBaseName_nullOrEmpty(String input) {
        assertThat(FileUtils.getBaseName(input)).isEqualTo(input);
    }

    // --- generateUniqueFilename ---

    @Test
    void generateUniqueFilename_preservesExtension() {
        String result = FileUtils.generateUniqueFilename("photo.png");
        assertThat(result).endsWith(".png");
        assertThat(result).hasSize(36 + 4); // UUID + ".png"
    }

    @Test
    void generateUniqueFilename_noExtension() {
        String result = FileUtils.generateUniqueFilename("noext");
        assertThat(result).hasSize(36); // UUID only
    }

    // --- sanitizeFilename ---

    @Test
    void sanitizeFilename_replacesIllegalChars() {
        assertThat(FileUtils.sanitizeFilename("file:name?.txt")).isEqualTo("file_name_.txt");
        assertThat(FileUtils.sanitizeFilename("a<b>c|d")).isEqualTo("a_b_c_d");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void sanitizeFilename_nullOrEmpty(String input) {
        assertThat(FileUtils.sanitizeFilename(input)).isEqualTo(input);
    }

    // --- formatFileSize ---

    @ParameterizedTest
    @CsvSource({
            "0, 0 B",
            "512, 512 B",
            "1023, 1023 B"
    })
    void formatFileSize_bytes(long bytes, String expected) {
        assertThat(FileUtils.formatFileSize(bytes)).isEqualTo(expected);
    }

    @Test
    void formatFileSize_kilobytes() {
        assertThat(FileUtils.formatFileSize(1024)).isEqualTo("1.0 KB");
        assertThat(FileUtils.formatFileSize(1536)).isEqualTo("1.5 KB");
    }

    @Test
    void formatFileSize_megabytes() {
        assertThat(FileUtils.formatFileSize(1048576)).isEqualTo("1.0 MB");
    }

    @Test
    void formatFileSize_gigabytes() {
        assertThat(FileUtils.formatFileSize(1073741824L)).isEqualTo("1.0 GB");
    }

    // --- MIME type helpers ---

    @Test
    void isImage() {
        assertThat(FileUtils.isImage("image/png")).isTrue();
        assertThat(FileUtils.isImage("image/jpeg")).isTrue();
        assertThat(FileUtils.isImage("text/plain")).isFalse();
        assertThat(FileUtils.isImage(null)).isFalse();
    }

    @Test
    void isVideo() {
        assertThat(FileUtils.isVideo("video/mp4")).isTrue();
        assertThat(FileUtils.isVideo("image/png")).isFalse();
        assertThat(FileUtils.isVideo(null)).isFalse();
    }

    @Test
    void isDocument() {
        assertThat(FileUtils.isDocument("application/pdf")).isTrue();
        assertThat(FileUtils.isDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")).isTrue();
        assertThat(FileUtils.isDocument("application/msword")).isTrue();
        assertThat(FileUtils.isDocument("text/plain")).isTrue();
        assertThat(FileUtils.isDocument("image/png")).isFalse();
        assertThat(FileUtils.isDocument(null)).isFalse();
    }

    @Test
    void detectFileType() {
        assertThat(FileUtils.detectFileType("image/png")).isEqualTo("image");
        assertThat(FileUtils.detectFileType("video/mp4")).isEqualTo("video");
        assertThat(FileUtils.detectFileType("application/pdf")).isEqualTo("document");
        assertThat(FileUtils.detectFileType("application/octet-stream")).isEqualTo("other");
    }
}
