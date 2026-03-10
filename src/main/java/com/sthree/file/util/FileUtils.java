package com.sthree.file.util;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for file operations.
 * 
 * Provides helper methods for file name processing,
 * extension detection, and other common operations.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
public final class FileUtils {

    private FileUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get the file extension from a filename.
     * 
     * @param filename the filename
     * @return Optional containing the extension (without dot), or empty if none
     */
    public static Optional<String> getExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return Optional.empty();
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return Optional.of(filename.substring(lastDotIndex + 1).toLowerCase());
        }
        
        return Optional.empty();
    }

    /**
     * Get the filename without extension.
     * 
     * @param filename the filename
     * @return filename without extension, or original if no extension
     */
    public static String getBaseName(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(0, lastDotIndex);
        }
        
        return filename;
    }

    /**
     * Generate a unique filename.
     * 
     * @param originalFilename the original filename
     * @return unique filename with UUID
     */
    public static String generateUniqueFilename(String originalFilename) {
        String extension = getExtension(originalFilename).map(ext -> "." + ext).orElse("");
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Sanitize a filename by removing unsafe characters.
     * 
     * @param filename the filename to sanitize
     * @return sanitized filename
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }
        
        // Remove path separators and other unsafe characters
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * Format file size as human-readable string.
     * 
     * @param bytes file size in bytes
     * @return human-readable size (e.g., "1.5 MB")
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Determine if a MIME type is an image.
     * 
     * @param mimeType the MIME type
     * @return true if image type
     */
    public static boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Determine if a MIME type is a video.
     * 
     * @param mimeType the MIME type
     * @return true if video type
     */
    public static boolean isVideo(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * Determine if a MIME type is a document.
     * 
     * @param mimeType the MIME type
     * @return true if document type
     */
    public static boolean isDocument(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return mimeType.equals("application/pdf") ||
               mimeType.startsWith("application/vnd.") ||
               mimeType.startsWith("application/msword") ||
               mimeType.startsWith("text/");
    }

    /**
     * Detect file type from MIME type.
     * 
     * @param mimeType the MIME type
     * @return file type string (image, video, document, other)
     */
    public static String detectFileType(String mimeType) {
        if (isImage(mimeType)) {
            return "image";
        } else if (isVideo(mimeType)) {
            return "video";
        } else if (isDocument(mimeType)) {
            return "document";
        }
        return "other";
    }
}
