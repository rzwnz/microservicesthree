package com.sthree.file.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing metadata key-value pairs for a file.
 * 
 * Stores additional metadata about files such as image dimensions,
 * description, tags, and other custom properties.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataEntity {

    /**
     * Unique identifier for the metadata entry.
     */
    private UUID id;

    /**
     * Reference to the file this metadata belongs to.
     */
    private UUID fileId;

    /**
     * Metadata key name.
     */
    private String metadataKey;

    /**
     * Metadata value.
     */
    private String metadataValue;

    /**
     * Timestamp when the metadata was created.
     */
    private LocalDateTime createdAt;

    /**
     * Common metadata keys used in the system.
     */
    public static class MetadataKey {
        public static final String WIDTH = "width";
        public static final String HEIGHT = "height";
        public static final String DURATION = "duration";
        public static final String DESCRIPTION = "description";
        public static final String TAGS = "tags";
        public static final String LANGUAGE = "language";
        public static final String SOURCE_LINK = "source_link";
        public static final String EXIF_DATA = "exif_data";
        public static final String THUMBNAIL_PATH = "thumbnail_path";
    }

    /**
     * Create a new metadata entry for a file.
     */
    public static FileMetadataEntity of(UUID fileId, String key, String value) {
        return FileMetadataEntity.builder()
                .id(UUID.randomUUID())
                .fileId(fileId)
                .metadataKey(key)
                .metadataValue(value)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
