package com.sthree.file.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a thumbnail for an image file.
 * 
 * Stores information about generated thumbnails for images,
 * including different sizes for various use cases.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileThumbnailEntity {

    /**
     * Unique identifier for the thumbnail.
     */
    private UUID id;

    /**
     * Reference to the original file.
     */
    private UUID fileId;

    /**
     * Path to the thumbnail in object storage.
     */
    private String thumbnailPath;

    /**
     * Size category: small, medium, large.
     */
    private ThumbnailSize thumbnailSize;

    /**
     * Actual width of the thumbnail in pixels.
     */
    private Integer width;

    /**
     * Actual height of the thumbnail in pixels.
     */
    private Integer height;

    /**
     * Timestamp when the thumbnail was created.
     */
    private LocalDateTime createdAt;

    /**
     * Enumeration of thumbnail sizes.
     */
    public enum ThumbnailSize {
        SMALL("small", 150, 150),
        MEDIUM("medium", 300, 300),
        LARGE("large", 600, 600);

        private final String value;
        private final int defaultWidth;
        private final int defaultHeight;

        ThumbnailSize(String value, int defaultWidth, int defaultHeight) {
            this.value = value;
            this.defaultWidth = defaultWidth;
            this.defaultHeight = defaultHeight;
        }

        public String getValue() {
            return value;
        }

        public int getDefaultWidth() {
            return defaultWidth;
        }

        public int getDefaultHeight() {
            return defaultHeight;
        }

        public static ThumbnailSize fromString(String value) {
            for (ThumbnailSize size : ThumbnailSize.values()) {
                if (size.value.equalsIgnoreCase(value)) {
                    return size;
                }
            }
            throw new IllegalArgumentException("Unknown thumbnail size: " + value);
        }
    }

    /**
     * Create a new thumbnail entry.
     */
    public static FileThumbnailEntity of(UUID fileId, String thumbnailPath, 
                                          ThumbnailSize size, int width, int height) {
        return FileThumbnailEntity.builder()
                .id(UUID.randomUUID())
                .fileId(fileId)
                .thumbnailPath(thumbnailPath)
                .thumbnailSize(size)
                .width(width)
                .height(height)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
