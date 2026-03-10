package com.sthree.file.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sthree.file.config.GarageProperties;
import com.sthree.file.config.PresignedUrlProperties;
import com.sthree.file.exception.StorageException;
import com.sthree.file.service.StorageService;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock S3Client s3Client;
    @Mock S3Presigner s3Presigner;
    @Mock GarageProperties garageProperties;
    @Mock PresignedUrlProperties presignedUrlProperties;

    @InjectMocks StorageService storageService;

    @Test
    void uploadFile_inputStream_success() {
        byte[] content = "data".getBytes();
        InputStream is = new ByteArrayInputStream(content);

        String result = storageService.uploadFile("bucket", "key", is, "text/plain", content.length);

        assertThat(result).isEqualTo("key");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFile_inputStream_failure_throwsStorageException() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 error"));

        assertThatThrownBy(() ->
                storageService.uploadFile("bucket", "key", new ByteArrayInputStream(new byte[0]), "text/plain", 0))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void uploadFile_bytes_success() {
        byte[] content = "hello".getBytes();

        String result = storageService.uploadFile("bucket", "key", content, "text/plain");

        assertThat(result).isEqualTo("key");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFile_bytes_failure_throwsStorageException() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 error"));

        assertThatThrownBy(() ->
                storageService.uploadFile("bucket", "key", "data".getBytes(), "text/plain"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void downloadFile_success() {
        byte[] expected = "file data".getBytes();
        GetObjectResponse response = GetObjectResponse.builder().build();
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response, expected);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        byte[] result = storageService.downloadFile("bucket", "key");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void downloadFile_failure_throwsStorageException() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(new RuntimeException("download error"));

        assertThatThrownBy(() -> storageService.downloadFile("bucket", "key"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void deleteFile_success() {
        storageService.deleteFile("bucket", "key");

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteFile_failure_throwsStorageException() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(new RuntimeException("delete error"));

        assertThatThrownBy(() -> storageService.deleteFile("bucket", "key"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void deleteFiles_emptyList_doesNothing() {
        storageService.deleteFiles("bucket", Collections.emptyList());
        storageService.deleteFiles("bucket", null);

        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    void deleteFiles_success() {
        storageService.deleteFiles("bucket", List.of("key1", "key2"));

        verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    void deleteFiles_failure_throwsStorageException() {
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenThrow(new RuntimeException("batch delete error"));

        assertThatThrownBy(() -> storageService.deleteFiles("bucket", List.of("k1")))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void fileExists_true() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        assertThat(storageService.fileExists("bucket", "key")).isTrue();
    }

    @Test
    void fileExists_noSuchKey_false() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Not found").build());

        assertThat(storageService.fileExists("bucket", "key")).isFalse();
    }

    @Test
    void fileExists_otherException_false() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(new RuntimeException("other"));

        assertThat(storageService.fileExists("bucket", "key")).isFalse();
    }

    @Test
    void getFileMetadata_success() {
        HeadObjectResponse expected = HeadObjectResponse.builder().contentLength(100L).build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(expected);

        HeadObjectResponse result = storageService.getFileMetadata("bucket", "key");

        assertThat(result.contentLength()).isEqualTo(100L);
    }

    @Test
    void getFileMetadata_noSuchKey_returnsNull() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Not found").build());

        assertThat(storageService.getFileMetadata("bucket", "key")).isNull();
    }

    @Test
    void getFileMetadata_otherError_throwsStorageException() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(new RuntimeException("err"));

        assertThatThrownBy(() -> storageService.getFileMetadata("bucket", "key"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void generatePresignedDownloadUrl_success() throws Exception {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("http://example.com/download"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = storageService.generatePresignedDownloadUrl("bucket", "key", 3600L);

        assertThat(url).isEqualTo("http://example.com/download");
    }

    @Test
    void generatePresignedDownloadUrl_failure_throwsStorageException() {
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(new RuntimeException("presign error"));

        assertThatThrownBy(() -> storageService.generatePresignedDownloadUrl("bucket", "key", 3600))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void generatePresignedUploadUrl_success() throws Exception {
        when(presignedUrlProperties.getUploadExpirationSeconds()).thenReturn(1800L);
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("http://example.com/upload"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

        String url = storageService.generatePresignedUploadUrl("bucket", "key", "text/plain");

        assertThat(url).isEqualTo("http://example.com/upload");
    }

    @Test
    void generatePresignedUploadUrl_failure_throwsStorageException() {
        when(presignedUrlProperties.getUploadExpirationSeconds()).thenReturn(1800L);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenThrow(new RuntimeException("presign error"));

        assertThatThrownBy(() -> storageService.generatePresignedUploadUrl("bucket", "key", "text/plain"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void createBucketIfNotExists_creates() {
        ListBucketsResponse listResp = ListBucketsResponse.builder()
                .buckets(Collections.emptyList())
                .build();
        when(s3Client.listBuckets()).thenReturn(listResp);

        storageService.createBucketIfNotExists("new-bucket");

        verify(s3Client).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void createBucketIfNotExists_alreadyExists() {
        Bucket existingBucket = Bucket.builder().name("existing").build();
        ListBucketsResponse listResp = ListBucketsResponse.builder()
                .buckets(List.of(existingBucket))
                .build();
        when(s3Client.listBuckets()).thenReturn(listResp);

        storageService.createBucketIfNotExists("existing");

        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void createBucketIfNotExists_failure_throwsStorageException() {
        when(s3Client.listBuckets()).thenThrow(new RuntimeException("err"));

        assertThatThrownBy(() -> storageService.createBucketIfNotExists("bucket"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void generateStoragePath_withExtension() {
        UUID uid = UUID.randomUUID();
        String path = storageService.generateStoragePath(uid, "media", "photo.jpg");

        assertThat(path).startsWith("entities/by-id/" + uid + "/media/");
        assertThat(path).endsWith(".jpg");
    }

    @Test
    void generateStoragePath_noExtension() {
        UUID uid = UUID.randomUUID();
        String path = storageService.generateStoragePath(uid, "files", "noext");

        assertThat(path).startsWith("entities/by-id/" + uid + "/files/");
        assertThat(path).doesNotContain(".");
    }

    @Test
    void generateStoragePath_nullCategory_defaultsToFiles() {
        UUID uid = UUID.randomUUID();
        String path = storageService.generateStoragePath(uid, null, "doc.pdf");

        assertThat(path).startsWith("entities/by-id/" + uid + "/files/");
        assertThat(path).endsWith(".pdf");
    }

    @Test
    void getDataBucket_returnsBucketFromProperties() {
        when(garageProperties.getDataBucket()).thenReturn("my-data-bucket");
        assertThat(storageService.getDataBucket()).isEqualTo("my-data-bucket");
    }

    @Test
    void getDownloadUrlExpiration_returnsInFuture() {
        when(presignedUrlProperties.getDownloadExpirationSeconds()).thenReturn(3600L);
        LocalDateTime exp = storageService.getDownloadUrlExpiration();
        assertThat(exp).isAfter(LocalDateTime.now());
    }

    @Test
    void getUploadUrlExpiration_returnsInFuture() {
        when(presignedUrlProperties.getUploadExpirationSeconds()).thenReturn(1800L);
        LocalDateTime exp = storageService.getUploadUrlExpiration();
        assertThat(exp).isAfter(LocalDateTime.now());
    }
}
