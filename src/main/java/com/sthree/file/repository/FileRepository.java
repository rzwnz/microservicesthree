package com.sthree.file.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sthree.file.entity.FileEntity;
import com.sthree.file.exception.FileNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.*;

/**
 * Repository for file entity operations using JOOQ.
 * 
 * Provides data access methods for the files table with type-safe
 * query building and efficient database operations.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FileRepository {

    private final DSLContext dsl;
    private static final String FILES_TABLE = "files";

    /**
     * Insert a new file record.
     * 
     * @param file the file entity to insert
     * @return the inserted file entity with generated ID
     */
    @Transactional
    public FileEntity insert(FileEntity file) {
        if (file.getId() == null) {
            file.setId(UUID.randomUUID());
        }
        if (file.getCreatedAt() == null) {
            file.setCreatedAt(LocalDateTime.now());
        }
        file.setUpdatedAt(LocalDateTime.now());

        dsl.insertInto(table(FILES_TABLE))
                .set(field("id"), file.getId())
                .set(field("file_name"), file.getFileName())
                .set(field("original_name"), file.getOriginalName())
                .set(field("file_size"), file.getFileSize())
                .set(field("file_type"), file.getFileType())
                .set(field("mime_type"), file.getMimeType())
                .set(field("storage_path"), file.getStoragePath())
                .set(field("bucket_name"), file.getBucketName())
                .set(field("uploaded_by"), file.getUploadedBy())
                .set(field("access_level"), file.getAccessLevel() != null ? 
                        file.getAccessLevel().getValue() : null)
                .set(field("checksum"), file.getChecksum())
                .set(field("created_at"), file.getCreatedAt())
                .set(field("updated_at"), file.getUpdatedAt())
                .set(field("deleted_at"), file.getDeletedAt())
                .execute();

        log.debug("Inserted file record with ID: {}", file.getId());
        return file;
    }

    /**
     * Find a file by ID.
     * 
     * @param id the file ID
     * @return Optional containing the file if found
     */
    public Optional<FileEntity> findById(UUID id) {
        Record record = dsl.selectFrom(table(FILES_TABLE))
                .where(field("id").eq(id))
                .and(field("deleted_at").isNull())
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToEntity);
    }

    /**
     * Find a file by ID, including soft-deleted files.
     * 
     * @param id the file ID
     * @return Optional containing the file if found
     */
    public Optional<FileEntity> findByIdIncludingDeleted(UUID id) {
        Record record = dsl.selectFrom(table(FILES_TABLE))
                .where(field("id").eq(id))
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToEntity);
    }

    /**
     * Find all files uploaded by a user.
     * 
     * @param userId the user ID
     * @return list of files uploaded by the user
     */
    public List<FileEntity> findByUploadedBy(UUID userId) {
        Result<Record> records = dsl.selectFrom(table(FILES_TABLE))
                .where(field("uploaded_by").eq(userId))
                .and(field("deleted_at").isNull())
                .orderBy(field("created_at").desc())
                .fetch();

        return records.map(this::mapToEntity);
    }

    /**
     * Find all files uploaded by a user with pagination.
     * 
     * @param userId the user ID
     * @param offset pagination offset
     * @param limit pagination limit
     * @return list of files uploaded by the user
     */
    public List<FileEntity> findByUploadedBy(UUID userId, int offset, int limit) {
        Result<Record> records = dsl.selectFrom(table(FILES_TABLE))
                .where(field("uploaded_by").eq(userId))
                .and(field("deleted_at").isNull())
                .orderBy(field("created_at").desc())
                .limit(limit)
                .offset(offset)
                .fetch();

        return records.map(this::mapToEntity);
    }

    /**
     * Find files by type.
     * 
     * @param fileType the file type
     * @return list of files of the specified type
     */
    public List<FileEntity> findByFileType(String fileType) {
        Result<Record> records = dsl.selectFrom(table(FILES_TABLE))
                .where(field("file_type").eq(fileType))
                .and(field("deleted_at").isNull())
                .orderBy(field("created_at").desc())
                .fetch();

        return records.map(this::mapToEntity);
    }

    /**
     * Update a file's access level.
     * 
     * @param id the file ID
     * @param accessLevel the new access level
     * @return true if updated successfully
     */
    @Transactional
    public boolean updateAccessLevel(UUID id, FileEntity.AccessLevel accessLevel) {
        int updated = dsl.update(table(FILES_TABLE))
                .set(field("access_level"), accessLevel.getValue())
                .set(field("updated_at"), LocalDateTime.now())
                .where(field("id").eq(id))
                .execute();

        log.debug("Updated access level for file {}: {}", id, accessLevel);
        return updated > 0;
    }

    /**
     * Soft delete a file.
     * 
     * @param id the file ID
     * @return true if deleted successfully
     */
    @Transactional
    public boolean softDelete(UUID id) {
        int updated = dsl.update(table(FILES_TABLE))
                .set(field("deleted_at"), LocalDateTime.now())
                .set(field("updated_at"), LocalDateTime.now())
                .where(field("id").eq(id))
                .and(field("deleted_at").isNull())
                .execute();

        log.debug("Soft deleted file: {}", id);
        return updated > 0;
    }

    /**
     * Permanently delete a file.
     * 
     * @param id the file ID
     * @return true if deleted successfully
     */
    @Transactional
    public boolean hardDelete(UUID id) {
        int deleted = dsl.deleteFrom(table(FILES_TABLE))
                .where(field("id").eq(id))
                .execute();

        log.debug("Hard deleted file: {}", id);
        return deleted > 0;
    }

    /**
     * Calculate total storage used by a user.
     * 
     * @param userId the user ID
     * @return total bytes used
     */
    public long calculateStorageUsed(UUID userId) {
        Long total = dsl.select(sum(field("file_size", Long.class)))
                .from(table(FILES_TABLE))
                .where(field("uploaded_by").eq(userId))
                .and(field("deleted_at").isNull())
                .fetchOne(0, Long.class);

        return total != null ? total : 0L;
    }

    /**
     * Count files uploaded by a user.
     * 
     * @param userId the user ID
     * @return number of files
     */
    public long countByUploadedBy(UUID userId) {
        Long count = dsl.selectCount()
                .from(table(FILES_TABLE))
                .where(field("uploaded_by").eq(userId))
                .and(field("deleted_at").isNull())
                .fetchOne(0, Long.class);

        return count != null ? count : 0L;
    }

    /**
     * Find files for cleanup (soft-deleted files older than retention period).
     * 
     * @param retentionDate files deleted before this date should be cleaned up
     * @return list of files to clean up
     */
    public List<FileEntity> findFilesForCleanup(LocalDateTime retentionDate) {
        Result<Record> records = dsl.selectFrom(table(FILES_TABLE))
                .where(field("deleted_at").isNotNull())
                .and(field("deleted_at").lt(retentionDate))
                .fetch();

        return records.map(this::mapToEntity);
    }

    /**
     * Map a database record to a FileEntity.
     */
    private FileEntity mapToEntity(Record record) {
        String accessLevelStr = record.get(field("access_level", String.class));
        FileEntity.AccessLevel accessLevel = accessLevelStr != null ?
                FileEntity.AccessLevel.fromString(accessLevelStr) : null;

        return FileEntity.builder()
                .id(record.get(field("id", UUID.class)))
                .fileName(record.get(field("file_name", String.class)))
                .originalName(record.get(field("original_name", String.class)))
                .fileSize(record.get(field("file_size", Long.class)))
                .fileType(record.get(field("file_type", String.class)))
                .mimeType(record.get(field("mime_type", String.class)))
                .storagePath(record.get(field("storage_path", String.class)))
                .bucketName(record.get(field("bucket_name", String.class)))
                .uploadedBy(record.get(field("uploaded_by", UUID.class)))
                .accessLevel(accessLevel)
                .checksum(record.get(field("checksum", String.class)))
                .createdAt(record.get(field("created_at", LocalDateTime.class)))
                .updatedAt(record.get(field("updated_at", LocalDateTime.class)))
                .deletedAt(record.get(field("deleted_at", LocalDateTime.class)))
                .build();
    }
}
