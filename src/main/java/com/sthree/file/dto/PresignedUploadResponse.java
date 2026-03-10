package com.sthree.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for a presigned upload URL.
 * 
 * Contains the presigned URL and storage coordinates needed
 * for the client to upload directly to S3/Garage.
 * After uploading, the client must call /confirm-upload
 * with the same storagePath and bucketName.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUploadResponse {

    /**
     * Presigned PUT URL for direct upload.
     */
    private String uploadUrl;

    /**
     * Storage path (object key) assigned to the upload.
     */
    private String storagePath;

    /**
     * Bucket the file will be stored in.
     */
    private String bucketName;

    /**
     * When the presigned URL expires.
     */
    private LocalDateTime expiresAt;
}
