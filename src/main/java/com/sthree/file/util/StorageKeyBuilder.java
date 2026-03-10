package com.sthree.file.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Centralised builder for S3 / Garage object keys.
 *
 * <p>Follows the <strong>application-agnostic</strong> layout defined in
 * {@code adjustments.md}:
 * <pre>
 *   {resourceType}/{scope}/{resourceId}/{category}/{objectId}.{ext}
 * </pre>
 *
 * <h3>Resource types</h3>
 * <ul>
 *   <li>{@code entities} – data associated with any entity (user, project, group …)</li>
 *   <li>{@code artifacts} – generated files, reports, exports</li>
 *   <li>{@code system}   – backups, snapshots, internal system data</li>
 *   <li>{@code tmp}      – temporary / staging data</li>
 * </ul>
 *
 * @author rzwnz
 * @version 1.0.0
 * @see <a href="adjustments.md">Object-Storage Layout – adjustments.md</a>
 */
public final class StorageKeyBuilder {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private StorageKeyBuilder() {
        // utility class
    }

    // ── entities/ ──────────────────────────────────────────────

    /**
     * Build a key for an entity-scoped object.
     *
     * <p>Pattern: {@code entities/by-id/{entityId}/{category}/{objectId}.{ext}}
     *
     * @param entityId UUID of the owning entity (user, group, project …)
     * @param category neutral category: {@code media}, {@code attachments}, {@code files}, {@code metadata}
     * @param originalFileName original file name (used to derive extension)
     * @return the full object key (no leading slash)
     */
    public static String entityObject(UUID entityId, String category, String originalFileName) {
        String ext = extensionOf(originalFileName);
        String objectId = UUID.randomUUID().toString();
        return "entities/by-id/" + entityId + "/" + category + "/" + objectId + ext;
    }

    /**
     * Build a key for an entity-scoped object with a specific object id.
     *
     * @param entityId UUID of the owning entity
     * @param category neutral category
     * @param objectId specific object id to use
     * @param originalFileName original file name (used to derive extension)
     * @return the full object key
     */
    public static String entityObject(UUID entityId, String category, UUID objectId, String originalFileName) {
        String ext = extensionOf(originalFileName);
        return "entities/by-id/" + entityId + "/" + category + "/" + objectId + ext;
    }

    /**
     * Convenience: entity media (images, avatars).
     */
    public static String entityMedia(UUID entityId, String originalFileName) {
        return entityObject(entityId, "media", originalFileName);
    }

    /**
     * Convenience: entity attachments (chat files, documents).
     */
    public static String entityAttachment(UUID entityId, String originalFileName) {
        return entityObject(entityId, "attachments", originalFileName);
    }

    /**
     * Convenience: entity generic files.
     */
    public static String entityFile(UUID entityId, String originalFileName) {
        return entityObject(entityId, "files", originalFileName);
    }

    // ── artifacts/ ─────────────────────────────────────────────

    /**
     * Build a key for an artifact scoped by ID.
     *
     * <p>Pattern: {@code artifacts/by-id/{artifactId}/files/{objectId}.{ext}}
     */
    public static String artifactById(UUID artifactId, String originalFileName) {
        String ext = extensionOf(originalFileName);
        String objectId = UUID.randomUUID().toString();
        return "artifacts/by-id/" + artifactId + "/files/" + objectId + ext;
    }

    /**
     * Build a key for a dated artifact (reports, exports).
     *
     * <p>Pattern: {@code artifacts/by-date/YYYY/MM/dd/reports/{artifactId}.{ext}}
     */
    public static String artifactByDate(LocalDate date, UUID artifactId, String category, String originalFileName) {
        String ext = extensionOf(originalFileName);
        return "artifacts/by-date/" + date.format(DATE_PATH) + "/" + category + "/" + artifactId + ext;
    }

    // ── system/ ────────────────────────────────────────────────

    /**
     * Build a system backup path.
     *
     * <p>Pattern: {@code system/backups/YYYY/MM/dd/{fileName}}
     */
    public static String systemBackup(LocalDate date, String fileName) {
        return "system/backups/" + date.format(DATE_PATH) + "/" + sanitize(fileName);
    }

    /**
     * Build a system snapshot path.
     *
     * <p>Pattern: {@code system/snapshots/{snapshotId}/{objectId}.{ext}}
     */
    public static String systemSnapshot(UUID snapshotId, String originalFileName) {
        String ext = extensionOf(originalFileName);
        String objectId = UUID.randomUUID().toString();
        return "system/snapshots/" + snapshotId + "/" + objectId + ext;
    }

    // ── tmp/ ───────────────────────────────────────────────────

    /**
     * Build a temporary upload path.
     *
     * <p>Pattern: {@code tmp/uploads/{uploadId}/{fileName}}
     */
    public static String tempUpload(UUID uploadId, String originalFileName) {
        return "tmp/uploads/" + uploadId + "/" + sanitize(originalFileName);
    }

    /**
     * Build a temporary processing path.
     *
     * <p>Pattern: {@code tmp/processing/{jobId}/{fileName}}
     */
    public static String tempProcessing(UUID jobId, String originalFileName) {
        return "tmp/processing/" + jobId + "/" + sanitize(originalFileName);
    }

    // ── helpers ────────────────────────────────────────────────

    /**
     * Extract the file extension (including the dot) from a filename.
     *
     * @return extension with dot (e.g. {@code ".png"}) or empty string
     */
    static String extensionOf(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot > 0 && dot < filename.length() - 1) {
            return filename.substring(dot).toLowerCase();
        }
        return "";
    }

    /**
     * Sanitize a filename by stripping path traversal and unsafe characters.
     */
    static String sanitize(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unnamed";
        }
        // Keep only safe chars
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
