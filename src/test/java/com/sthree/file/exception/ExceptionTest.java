package com.sthree.file.exception;

import org.junit.jupiter.api.Test;

import com.sthree.file.exception.FileAccessDeniedException;
import com.sthree.file.exception.FileNotFoundException;
import com.sthree.file.exception.FileServiceException;
import com.sthree.file.exception.FileShareException;
import com.sthree.file.exception.FileUploadException;
import com.sthree.file.exception.FileValidationException;
import com.sthree.file.exception.QuotaExceededException;
import com.sthree.file.exception.StorageException;
import com.sthree.file.exception.UnauthorizedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionTest {

    // --- FileServiceException ---

    @Test
    void fileServiceException_twoArg() {
        FileServiceException ex = new FileServiceException("msg", "CODE");
        assertThat(ex.getMessage()).isEqualTo("msg");
        assertThat(ex.getErrorCode()).isEqualTo("CODE");
        assertThat(ex.getStatusCode()).isEqualTo(500);
    }

    @Test
    void fileServiceException_threeArg() {
        FileServiceException ex = new FileServiceException("msg", "CODE", 409);
        assertThat(ex.getStatusCode()).isEqualTo(409);
    }

    @Test
    void fileServiceException_withCause() {
        RuntimeException cause = new RuntimeException("root");
        FileServiceException ex = new FileServiceException("msg", "CODE", cause);
        assertThat(ex.getCause()).isEqualTo(cause);
        assertThat(ex.getStatusCode()).isEqualTo(500);

        FileServiceException ex2 = new FileServiceException("msg", "CODE", 502, cause);
        assertThat(ex2.getStatusCode()).isEqualTo(502);
    }

    // --- StorageException ---

    @Test
    void storageException_message() {
        StorageException ex = new StorageException("failed");
        assertThat(ex.getErrorCode()).isEqualTo(FileServiceException.ErrorCode.STORAGE_ERROR);
        assertThat(ex.getStatusCode()).isEqualTo(500);
    }

    @Test
    void storageException_withCause() {
        Throwable cause = new RuntimeException("root");
        StorageException ex = new StorageException("failed", cause);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void storageException_factories() {
        RuntimeException cause = new RuntimeException();
        assertThat(StorageException.uploadFailed("f", cause).getMessage()).contains("f");
        assertThat(StorageException.downloadFailed("f", cause).getMessage()).contains("f");
        assertThat(StorageException.deleteFailed("f", cause).getMessage()).contains("f");
    }

    // --- FileNotFoundException ---

    @Test
    void fileNotFoundException_uuid() {
        UUID id = UUID.randomUUID();
        FileNotFoundException ex = new FileNotFoundException(id);
        assertThat(ex.getMessage()).contains(id.toString());
        assertThat(ex.getStatusCode()).isEqualTo(404);
    }

    @Test
    void fileNotFoundException_message() {
        FileNotFoundException ex = new FileNotFoundException("custom msg");
        assertThat(ex.getMessage()).isEqualTo("custom msg");
    }

    // --- FileUploadException ---

    @Test
    void fileUploadException() {
        FileUploadException ex = new FileUploadException("upload failed");
        assertThat(ex.getStatusCode()).isEqualTo(500);
        assertThat(ex.getErrorCode()).isEqualTo(FileServiceException.ErrorCode.UPLOAD_FAILED);
    }

    @Test
    void fileUploadException_withCause() {
        RuntimeException cause = new RuntimeException("io");
        FileUploadException ex = new FileUploadException("failed", cause);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    // --- FileShareException ---

    @Test
    void fileShareException_expired() {
        FileShareException ex = FileShareException.expired("tok");
        assertThat(ex.getErrorCode()).isEqualTo(FileServiceException.ErrorCode.SHARE_EXPIRED);
        assertThat(ex.getStatusCode()).isEqualTo(400);
    }

    @Test
    void fileShareException_limitReached() {
        FileShareException ex = FileShareException.limitReached("tok");
        assertThat(ex.getErrorCode()).isEqualTo(FileServiceException.ErrorCode.SHARE_LIMIT_REACHED);
    }

    @Test
    void fileShareException_invalidToken() {
        FileShareException ex = FileShareException.invalidToken("tok");
        assertThat(ex.getErrorCode()).isEqualTo(FileServiceException.ErrorCode.INVALID_SHARE_TOKEN);
    }

    @Test
    void fileShareException_invalidPassword() {
        FileShareException ex = FileShareException.invalidPassword();
        assertThat(ex.getErrorCode()).isEqualTo(FileServiceException.ErrorCode.INVALID_PASSWORD);
    }

    // --- QuotaExceededException ---

    @Test
    void quotaExceededException() {
        QuotaExceededException ex = new QuotaExceededException(500L, 1024L, 600L);
        assertThat(ex.getStatusCode()).isEqualTo(413);
        assertThat(ex.getErrorCode()).isEqualTo(FileServiceException.ErrorCode.QUOTA_EXCEEDED);
        assertThat(ex.getMessage()).contains("500");
    }

    // --- FileValidationException ---

    @Test
    void fileValidationException_factories() {
        FileValidationException ex1 = FileValidationException.invalidFileContent("empty");
        assertThat(ex1.getStatusCode()).isEqualTo(400);

        FileValidationException ex2 = FileValidationException.fileTooLarge(1024L, 2048L);
        assertThat(ex2.getMessage()).contains("1024");

        FileValidationException ex3 = FileValidationException.invalidFileType("exe");
        assertThat(ex3.getMessage()).contains("exe");
    }

    // --- FileAccessDeniedException ---

    @Test
    void fileAccessDeniedException_forFile() {
        UUID fileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FileAccessDeniedException ex = FileAccessDeniedException.forFile(fileId, userId);
        assertThat(ex.getStatusCode()).isEqualTo(403);
    }

    @Test
    void fileAccessDeniedException_ownershipRequired() {
        FileAccessDeniedException ex = FileAccessDeniedException.ownershipRequired(
                UUID.randomUUID(), UUID.randomUUID());
        assertThat(ex.getStatusCode()).isEqualTo(403);
    }

    // --- UnauthorizedException ---

    @Test
    void unauthorizedException() {
        UnauthorizedException ex = new UnauthorizedException("no auth");
        assertThat(ex.getMessage()).isEqualTo("no auth");
        assertThat(ex.getStatusCode()).isEqualTo(401);
    }

    // --- ErrorCode constants ---

    @Test
    void errorCodeConstants() {
        assertThat(FileServiceException.ErrorCode.FILE_NOT_FOUND).isNotBlank();
        assertThat(FileServiceException.ErrorCode.ACCESS_DENIED).isNotBlank();
        assertThat(FileServiceException.ErrorCode.QUOTA_EXCEEDED).isNotBlank();
        assertThat(FileServiceException.ErrorCode.INTERNAL_ERROR).isNotBlank();
        assertThat(FileServiceException.ErrorCode.VALIDATION_ERROR).isNotBlank();
    }
}
