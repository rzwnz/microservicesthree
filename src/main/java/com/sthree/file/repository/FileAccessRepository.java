package com.sthree.file.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sthree.file.entity.FileAccessEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.*;

/**
 * Repository for file access control operations using JOOQ.
 * 
 * Provides data access methods for the file_access table,
 * managing user permissions for files.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FileAccessRepository {

    private final DSLContext dsl;
    private static final String ACCESS_TABLE = "file_access";

    /**
     * Insert a new access entry.
     * 
     * @param access the access entity to insert
     * @return the inserted access entity
     */
    @Transactional
    public FileAccessEntity insert(FileAccessEntity access) {
        if (access.getId() == null) {
            access.setId(UUID.randomUUID());
        }
        if (access.getGrantedAt() == null) {
            access.setGrantedAt(LocalDateTime.now());
        }

        dsl.insertInto(table(ACCESS_TABLE))
                .set(field("id"), access.getId())
                .set(field("file_id"), access.getFileId())
                .set(field("user_id"), access.getUserId())
                .set(field("access_type"), access.getAccessType().getValue())
                .set(field("granted_by"), access.getGrantedBy())
                .set(field("granted_at"), access.getGrantedAt())
                .set(field("expires_at"), access.getExpiresAt())
                .onConflict(field("file_id"), field("user_id"), field("access_type"))
                .doUpdate()
                .set(field("granted_at"), access.getGrantedAt())
                .set(field("expires_at"), access.getExpiresAt())
                .execute();

        log.debug("Inserted access for file {} to user {}: {}", 
                access.getFileId(), access.getUserId(), access.getAccessType());
        return access;
    }

    /**
     * Find all access entries for a file.
     * 
     * @param fileId the file ID
     * @return list of access entries
     */
    public List<FileAccessEntity> findByFileId(UUID fileId) {
        Result<Record> records = dsl.selectFrom(table(ACCESS_TABLE))
                .where(field("file_id").eq(fileId))
                .fetch();

        return records.map(this::mapToEntity);
    }

    /**
     * Find access entry for a specific file and user.
     * 
     * @param fileId the file ID
     * @param userId the user ID
     * @return list of access entries for the user on this file
     */
    public List<FileAccessEntity> findByFileIdAndUserId(UUID fileId, UUID userId) {
        Result<Record> records = dsl.selectFrom(table(ACCESS_TABLE))
                .where(field("file_id").eq(fileId))
                .and(field("user_id").eq(userId))
                .fetch();

        return records.map(this::mapToEntity);
    }

    /**
     * Find a specific access type for a file and user.
     * 
     * @param fileId the file ID
     * @param userId the user ID
     * @param accessType the access type
     * @return Optional containing the access if found
     */
    public Optional<FileAccessEntity> findByFileIdUserIdAndType(
            UUID fileId, UUID userId, FileAccessEntity.AccessType accessType) {
        
        Record record = dsl.selectFrom(table(ACCESS_TABLE))
                .where(field("file_id").eq(fileId))
                .and(field("user_id").eq(userId))
                .and(field("access_type").eq(accessType.getValue()))
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToEntity);
    }

    /**
     * Check if a user has a specific access type to a file.
     * 
     * @param fileId the file ID
     * @param userId the user ID
     * @param accessType the access type
     * @return true if the user has access
     */
    public boolean hasAccess(UUID fileId, UUID userId, FileAccessEntity.AccessType accessType) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(table(ACCESS_TABLE))
                        .where(field("file_id").eq(fileId))
                        .and(field("user_id").eq(userId))
                        .and(field("access_type").eq(accessType.getValue()))
                        .and(field("expires_at").isNull()
                                .or(field("expires_at").gt(LocalDateTime.now())))
        );
    }

    /**
     * Check if a user has any access to a file.
     * 
     * @param fileId the file ID
     * @param userId the user ID
     * @return true if the user has any access
     */
    public boolean hasAnyAccess(UUID fileId, UUID userId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(table(ACCESS_TABLE))
                        .where(field("file_id").eq(fileId))
                        .and(field("user_id").eq(userId))
                        .and(field("expires_at").isNull()
                                .or(field("expires_at").gt(LocalDateTime.now())))
        );
    }

    /**
     * Find all files a user has access to.
     * 
     * @param userId the user ID
     * @return list of access entries
     */
    public List<FileAccessEntity> findByUserId(UUID userId) {
        Result<Record> records = dsl.selectFrom(table(ACCESS_TABLE))
                .where(field("user_id").eq(userId))
                .and(field("expires_at").isNull()
                        .or(field("expires_at").gt(LocalDateTime.now())))
                .fetch();

        return records.map(this::mapToEntity);
    }

    /**
     * Delete an access entry.
     * 
     * @param fileId the file ID
     * @param userId the user ID
     * @param accessType the access type
     * @return true if deleted successfully
     */
    @Transactional
    public boolean delete(UUID fileId, UUID userId, FileAccessEntity.AccessType accessType) {
        int deleted = dsl.deleteFrom(table(ACCESS_TABLE))
                .where(field("file_id").eq(fileId))
                .and(field("user_id").eq(userId))
                .and(field("access_type").eq(accessType.getValue()))
                .execute();

        log.debug("Deleted access for file {} to user {}: {}", fileId, userId, accessType);
        return deleted > 0;
    }

    /**
     * Delete all access entries for a file.
     * 
     * @param fileId the file ID
     */
    @Transactional
    public void deleteByFileId(UUID fileId) {
        dsl.deleteFrom(table(ACCESS_TABLE))
                .where(field("file_id").eq(fileId))
                .execute();

        log.debug("Deleted all access entries for file {}", fileId);
    }

    /**
     * Delete all access entries for a user.
     * 
     * @param userId the user ID
     */
    @Transactional
    public void deleteByUserId(UUID userId) {
        dsl.deleteFrom(table(ACCESS_TABLE))
                .where(field("user_id").eq(userId))
                .execute();

        log.debug("Deleted all access entries for user {}", userId);
    }

    /**
     * Clean up expired access entries.
     * 
     * @return number of entries removed
     */
    @Transactional
    public int cleanupExpired() {
        int deleted = dsl.deleteFrom(table(ACCESS_TABLE))
                .where(field("expires_at").isNotNull())
                .and(field("expires_at").lt(LocalDateTime.now()))
                .execute();

        log.debug("Cleaned up {} expired access entries", deleted);
        return deleted;
    }

    /**
     * Map a database record to a FileAccessEntity.
     */
    private FileAccessEntity mapToEntity(Record record) {
        String accessTypeStr = record.get(field("access_type", String.class));
        
        return FileAccessEntity.builder()
                .id(record.get(field("id", UUID.class)))
                .fileId(record.get(field("file_id", UUID.class)))
                .userId(record.get(field("user_id", UUID.class)))
                .accessType(FileAccessEntity.AccessType.fromString(accessTypeStr))
                .grantedBy(record.get(field("granted_by", UUID.class)))
                .grantedAt(record.get(field("granted_at", LocalDateTime.class)))
                .expiresAt(record.get(field("expires_at", LocalDateTime.class)))
                .build();
    }
}
