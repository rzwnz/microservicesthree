package com.sthree.file.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.sthree.file.dto.ApiResponse;
import com.sthree.file.exception.FileNotFoundException;
import com.sthree.file.exception.FileServiceException;
import com.sthree.file.exception.GlobalExceptionHandler;
import com.sthree.file.exception.QuotaExceededException;
import com.sthree.file.exception.StorageException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleFileServiceException() {
        FileServiceException ex = new FileServiceException("test error", "TEST_CODE", 409);

        ResponseEntity<ApiResponse<Void>> response = handler.handleFileServiceException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).isEqualTo("test error");
        assertThat(response.getBody().getErrorCode()).isEqualTo("TEST_CODE");
    }

    @Test
    void handleFileNotFoundException() {
        FileNotFoundException ex = new FileNotFoundException(UUID.randomUUID());

        ResponseEntity<ApiResponse<Void>> response = handler.handleFileServiceException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getErrorCode()).isEqualTo(FileServiceException.ErrorCode.FILE_NOT_FOUND);
    }

    @Test
    void handleStorageException() {
        StorageException ex = new StorageException("storage failure");

        ResponseEntity<ApiResponse<Void>> response = handler.handleFileServiceException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getErrorCode()).isEqualTo(FileServiceException.ErrorCode.STORAGE_ERROR);
    }

    @Test
    void handleQuotaExceededException() {
        QuotaExceededException ex = new QuotaExceededException(500L, 1024L, 600L);

        ResponseEntity<ApiResponse<Void>> response = handler.handleFileServiceException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody().getErrorCode()).isEqualTo(FileServiceException.ErrorCode.QUOTA_EXCEEDED);
    }

    @Test
    void handleValidationExceptions() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "name", "must not be blank"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiResponse<Map<String, String>>> response = handler.handleValidationExceptions(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getData()).containsEntry("name", "must not be blank");
    }

    @Test
    void handleMaxSizeException() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(50 * 1024 * 1024);

        ResponseEntity<ApiResponse<Void>> response = handler.handleMaxSizeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void handleGenericException() {
        Exception ex = new RuntimeException("oops");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }
}
