package com.sthree.file.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sthree.file.entity.FileEntity;
import com.sthree.file.entity.FileShareEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.*;

/**
 * Repository for file share operations using JOOQ.
 * 
 * Provides data access methods for the file_shares table,
 * managing shareable links for files.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FileShareRepository {

    private final DSLContext dsl;
    private static final String SHARES_TABLE = "file_shares";

    /**
     * Insert a new share entry.
     * 
     * @param share the share entity to insert
     * @return the inserted share entity
     */
    @Transactional
    public FileShareEntity insert(FileShareEntity share) {
        if (share.getId() == null) {
            share.setId(UUID.randomUUID());
        }
        if (share.getCreatedAt() == null) {
            share.setCreatedAt(LocalDateTime.now());
        }
        if (share.getDownloadCount() == null) {
            share.setDownloadCount(0);
        }

        dsl.insertInto(table(SHARES_TABLE))
                .set(field("id"), share.getId())
                .set(field("file_id"), share.getFileId())
                .set(field("share_token"), share.getShareToken())
                .set(field("shared_by"), share.getSharedBy())
                .set(field("access_level"), share.getAccessLevel() != null ?
                        share.getAccessLevel().getValue() : null)
                .set(field("password_hash"), share.getPasswordHash())
                .set(field("expires_at"), share.getExpiresAt())
                .set(field("max_downloads"), share.getMaxDownloads())
                .set(field("download_count"), share.getDownloadCount())
                .set(field("created_at"), share.getCreatedAt())
                .execute();

        log.debug("Inserted share for file {} with token: {}", share.getFileId(), share.getShareToken());
        return share;
    }

    /**
     * Find a share by ID.
     * 
     * @param id the share ID
     * @return Optional containing the share if found
     */
    public Optional<FileShareEntity> findById(UUID id) {
        Record record = dsl.selectFrom(table(SHARES_TABLE))
                .where(field("id").eq(id))
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToEntity);
    }

    /**
     * Find a share by token.
     * 
     * @param token the share token
     * @return Optional containing the share if found
     */
    public Optional<FileShareEntity> findByToken(String token) {
        Record record = dsl.selectFrom(table(SHARES_TABLE))
                .where(field("share_token").eq(token))
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToEntity);
    }

    /**
     * Find all shares for a file.
     * 
     * @param fileId the file ID
     * @return list of shares for the file
     */
    public List<FileShareEntity> findByFileId(UUID fileId) {
        Result<Record> records = dsl.selectFrom(table(SHARES_TABLE))
                .where(field("file_id").eq(fileId))
                .orderBy(field("created_at").desc())
                .fetch();

        return records.map(this::mapToEntity);
    }

    /**
     * Find all shares created by a user.
     * 
     * @param userId the user ID
     * @return list of shares created by the user
     */
    public List<FileShareEntity> findBySharedBy(UUID userId) {
        Result<Record> records = dsl.selectFrom(table(SHARES_TABLE))
                .where(field("shared_by").eq(userId))
                .orderBy(field("created_at").desc())
                .fetch();

        return records.map(this::mapToEntity);
    }

    /**
     * Find active shares for a file.
     * 
     * @param fileId the file ID
     * @return list of active shares
     */
    public List<FileShareEntity> findActiveByFileId(UUID fileId) {
        Result<Record> records = dsl.selectFrom(table(SHARES_TABLE))
                .where(field("file_id").eq(fileId))
                .and(field("expires_at").isNull()
                        .or(field("expires_at").gt(LocalDateTime.now())))
                .orderBy(field("created_at").desc())
                .fetch();

        return records.map(this::mapToEntity);
    }

    /**
     * Increment download count.
     * 
     * @param token the share token
     * @return true if updated successfully
     */
    @Transactional
    public boolean incrementDownloadCount(String token) {
        int updated = dsl.update(table(SHARES_TABLE))
                .set(field("download_count", Integer.class), field("download_count", Integer.class).plus(1))
                .where(field("share_token").eq(token))
                .execute();

        log.debug("Incremented download count for share token: {}", token);
        return updated > 0;
    }

    /**
     * Delete a share.
     * 
     * @param id the share ID
     * @return true if deleted successfully
     */
    @Transactional
    public boolean delete(UUID id) {
        int deleted = dsl.deleteFrom(table(SHARES_TABLE))
                .where(field("id").eq(id))
                .execute();

        log.debug("Deleted share: {}", id);
        return deleted > 0;
    }

    /**
     * Delete a share by token.
     * 
     * @param token the share token
     * @return true if deleted successfully
     */
    @Transactional
    public boolean deleteByToken(String token) {
        int deleted = dsl.deleteFrom(table(SHARES_TABLE))
                .where(field("share_token").eq(token))
                .execute();

        log.debug("Deleted share with token: {}", token);
        return deleted > 0;
    }

    /**
     * Delete all shares for a file.
     * 
     * @param fileId the file ID
     */
    @Transactional
    public void deleteByFileId(UUID fileId) {
        dsl.deleteFrom(table(SHARES_TABLE))
                .where(field("file_id").eq(fileId))
                .execute();

        log.debug("Deleted all shares for file {}", fileId);
    }

    /**
     * Clean up expired shares.
     * 
     * @return number of shares removed
     */
    @Transactional
    public int cleanupExpired() {
        int deleted = dsl.deleteFrom(table(SHARES_TABLE))
                .where(field("expires_at").isNotNull())
                .and(field("expires_at").lt(LocalDateTime.now()))
                .execute();

        log.debug("Cleaned up {} expired shares", deleted);
        return deleted;
    }

    /**
     * Check if a share token exists.
     * 
     * @param token the share token
     * @return true if the token exists
     */
    public boolean existsByToken(String token) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(table(SHARES_TABLE))
                        .where(field("share_token").eq(token))
        );
    }

    /**
     * Map a database record to a FileShareEntity.
     */
    private FileShareEntity mapToEntity(Record record) {
        String accessLevelStr = record.get(field("access_level", String.class));
        FileEntity.AccessLevel accessLevel = accessLevelStr != null ?
                FileEntity.AccessLevel.fromString(accessLevelStr) : null;

        return FileShareEntity.builder()
                .id(record.get(field("id", UUID.class)))
                .fileId(record.get(field("file_id", UUID.class)))
                .shareToken(record.get(field("share_token", String.class)))
                .sharedBy(record.get(field("shared_by", UUID.class)))
                .accessLevel(accessLevel)
                .passwordHash(record.get(field("password_hash", String.class)))
                .expiresAt(record.get(field("expires_at", LocalDateTime.class)))
                .maxDownloads(record.get(field("max_downloads", Integer.class)))
                .downloadCount(record.get(field("download_count", Integer.class)))
                .createdAt(record.get(field("created_at", LocalDateTime.class)))
                .build();
    }
}
