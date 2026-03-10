package com.sthree.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Configuration properties for image processing settings.
 * 
 * Controls thumbnail generation, avatar processing, and image compression
 * settings.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "image")
public class ImageProcessingProperties {

    /**
     * Thumbnail generation settings.
     */
    private ThumbnailSettings thumbnail = new ThumbnailSettings();

    /**
     * Avatar processing settings.
     */
    private AvatarSettings avatar = new AvatarSettings();

    /**
     * Image compression settings.
     */
    private CompressionSettings compression = new CompressionSettings();

    /**
     * Thumbnail generation configuration.
     */
    @Data
    public static class ThumbnailSettings {
        /**
         * Whether thumbnail generation is enabled.
         */
        private boolean enabled = true;

        /**
         * Default thumbnail width in pixels.
         */
        @Min(value = 50, message = "Thumbnail width must be at least 50 pixels")
        @Max(value = 1000, message = "Thumbnail width must not exceed 1000 pixels")
        private int width = 200;

        /**
         * Default thumbnail height in pixels.
         */
        @Min(value = 50, message = "Thumbnail height must be at least 50 pixels")
        @Max(value = 1000, message = "Thumbnail height must not exceed 1000 pixels")
        private int height = 200;

        /**
         * JPEG quality for thumbnails (0-100).
         */
        @Min(value = 1, message = "Quality must be at least 1")
        @Max(value = 100, message = "Quality must not exceed 100")
        private int quality = 80;
    }

    /**
     * Avatar processing configuration.
     */
    @Data
    public static class AvatarSettings {
        /**
         * Maximum avatar width in pixels.
         */
        @Min(value = 100, message = "Avatar max width must be at least 100 pixels")
        private int maxWidth = 500;

        /**
         * Maximum avatar height in pixels.
         */
        @Min(value = 100, message = "Avatar max height must be at least 100 pixels")
        private int maxHeight = 500;

        /**
         * JPEG quality for avatars (0-100).
         */
        @Min(value = 1, message = "Quality must be at least 1")
        @Max(value = 100, message = "Quality must not exceed 100")
        private int quality = 85;
    }

    /**
     * Image compression configuration.
     */
    @Data
    public static class CompressionSettings {
        /**
         * Whether image compression is enabled.
         */
        private boolean enabled = true;

        /**
         * Maximum width for compressed images.
         */
        @Min(value = 100, message = "Compression max width must be at least 100 pixels")
        private int maxWidth = 1920;

        /**
         * Maximum height for compressed images.
         */
        @Min(value = 100, message = "Compression max height must be at least 100 pixels")
        private int maxHeight = 1080;

        /**
         * JPEG quality for compressed images (0-100).
         */
        @Min(value = 1, message = "Quality must be at least 1")
        @Max(value = 100, message = "Quality must not exceed 100")
        private int quality = 85;
    }

    /**
     * Thumbnail sizes enum for standard thumbnail dimensions.
     */
    public enum ThumbnailSize {
        SMALL(150, 150),
        MEDIUM(300, 300),
        LARGE(600, 600);

        private final int width;
        private final int height;

        ThumbnailSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}
