package com.sthree.file.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for file upload.
 *
 * <p>Designed to be <strong>domain-agnostic</strong>. The caller specifies
 * a generic {@link EntityContext} with an {@code entityId} and an optional
 * {@code category} instead of hard-coding chat/group/profile semantics.
 * 
 * @author rzwnz
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequest {

    /**
     * Type of file being uploaded: image, code, document.
     */
    @NotNull(message = "File type is required")
    private String type;

    /**
     * Domain-agnostic context for the upload (entity + category).
     */
    private EntityContext context;

    /**
     * Additional metadata for the file.
     */
    private FileMetadataRequest metadata;

    /**
     * Access level for the file: public, private, shared.
     */
    private String accessLevel;

    /**
     * Domain-agnostic context specifying the owning entity and
     * the storage category.
     *
     * <p>Examples:
     * <ul>
     *   <li>Avatar: {@code entityId = userId, category = "media"}</li>
     *   <li>Chat attachment: {@code entityId = chatId, category = "attachments"}</li>
     *   <li>Group image: {@code entityId = groupId, category = "media"}</li>
     * </ul>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityContext {
        /**
         * ID of the owning entity (user, group, project, …).
         */
        private UUID entityId;

        /**
         * Neutral storage category: media, attachments, files, metadata.
         * Defaults to "files" when null.
         */
        private String category;
    }

    /**
     * Metadata for the uploaded file.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileMetadataRequest {
        /**
         * Description of the file.
         */
        private String description;

        /**
         * Tags associated with the file.
         */
        private List<String> tags;
    }
}
