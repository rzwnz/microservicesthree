package com.sthree.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for file metadata.
 * 
 * Contains detailed metadata information about a file.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataResponse {

    /**
     * Unique identifier for the file.
     */
    private UUID fileId;

    /**
     * Detailed metadata about the file.
     */
    private FileMetadataDetails metadata;

    /**
     * Detailed metadata information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileMetadataDetails {
        /**
         * Original filename.
         */
        private String fileName;

        /**
         * File size in bytes.
         */
        private Long fileSize;

        /**
         * Type of file: image, code, document.
         */
        private String fileType;

        /**
         * MIME type of the file.
         */
        private String mimeType;

        /**
         * Image dimensions (if applicable).
         */
        private Dimensions dimensions;

        /**
         * Duration in seconds (for video/audio files).
         */
        private Integer duration;

        /**
         * Description of the file.
         */
        private String description;

        /**
         * Tags associated with the file.
         */
        private List<String> tags;

        /**
         * ID of user who uploaded the file.
         */
        private UUID uploadedBy;

        /**
         * When the file was uploaded.
         */
        private LocalDateTime uploadedAt;

        /**
         * When the file was last modified.
         */
        private LocalDateTime modifiedAt;

        /**
         * Additional custom metadata.
         */
        private Map<String, String> customMetadata;
    }

    /**
     * Image dimensions.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dimensions {
        private Integer width;
        private Integer height;
    }
}
