package com.sthree.file.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sthree.file.entity.FileMetadataEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;

/**
 * Repository for file metadata operations using JOOQ.
 * 
 * Provides data access methods for the file_metadata table,
 * storing key-value metadata pairs for files.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FileMetadataRepository {

    private final DSLContext dsl;
    private static final String METADATA_TABLE = "file_metadata";

    /**
     * Insert a new metadata entry.
     * 
     * @param metadata the metadata entity to insert
     * @return the inserted metadata entity
     */
    @Transactional
    public FileMetadataEntity insert(FileMetadataEntity metadata) {
        if (metadata.getId() == null) {
            metadata.setId(UUID.randomUUID());
        }
        if (metadata.getCreatedAt() == null) {
            metadata.setCreatedAt(LocalDateTime.now());
        }

        dsl.insertInto(table(METADATA_TABLE))
                .set(field("id"), metadata.getId())
                .set(field("file_id"), metadata.getFileId())
                .set(field("metadata_key"), metadata.getMetadataKey())
                .set(field("metadata_value"), metadata.getMetadataValue())
                .set(field("created_at"), metadata.getCreatedAt())
                .onConflict(field("file_id"), field("metadata_key"))
                .doUpdate()
                .set(field("metadata_value"), metadata.getMetadataValue())
                .execute();

        log.debug("Inserted metadata for file {}: {} = {}", 
                metadata.getFileId(), metadata.getMetadataKey(), metadata.getMetadataValue());
        return metadata;
    }

    /**
     * Insert multiple metadata entries for a file.
     * 
     * @param fileId the file ID
     * @param metadata map of key-value pairs
     */
    @Transactional
    public void insertBatch(UUID fileId, Map<String, String> metadata) {
        LocalDateTime now = LocalDateTime.now();
        
        metadata.forEach((key, value) -> {
            dsl.insertInto(table(METADATA_TABLE))
                    .set(field("id"), UUID.randomUUID())
                    .set(field("file_id"), fileId)
                    .set(field("metadata_key"), key)
                    .set(field("metadata_value"), value)
                    .set(field("created_at"), now)
                    .onConflict(field("file_id"), field("metadata_key"))
                    .doUpdate()
                    .set(field("metadata_value"), value)
                    .execute();
        });

        log.debug("Inserted {} metadata entries for file {}", metadata.size(), fileId);
    }

    /**
     * Find all metadata for a file.
     * 
     * @param fileId the file ID
     * @return list of metadata entries
     */
    public List<FileMetadataEntity> findByFileId(UUID fileId) {
        Result<Record> records = dsl.selectFrom(table(METADATA_TABLE))
                .where(field("file_id").eq(fileId))
                .fetch();

        return records.map(this::mapToEntity);
    }

    /**
     * Find metadata as a map for a file.
     * 
     * @param fileId the file ID
     * @return map of metadata key-value pairs
     */
    public Map<String, String> findMapByFileId(UUID fileId) {
        return findByFileId(fileId).stream()
                .collect(Collectors.toMap(
                        FileMetadataEntity::getMetadataKey,
                        FileMetadataEntity::getMetadataValue,
                        (existing, replacement) -> replacement
                ));
    }

    /**
     * Find a specific metadata entry.
     * 
     * @param fileId the file ID
     * @param key the metadata key
     * @return Optional containing the metadata if found
     */
    public Optional<FileMetadataEntity> findByFileIdAndKey(UUID fileId, String key) {
        Record record = dsl.selectFrom(table(METADATA_TABLE))
                .where(field("file_id").eq(fileId))
                .and(field("metadata_key").eq(key))
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToEntity);
    }

    /**
     * Get a metadata value.
     * 
     * @param fileId the file ID
     * @param key the metadata key
     * @return the metadata value, or null if not found
     */
    public String getValue(UUID fileId, String key) {
        return dsl.select(field("metadata_value"))
                .from(table(METADATA_TABLE))
                .where(field("file_id").eq(fileId))
                .and(field("metadata_key").eq(key))
                .fetchOne(field("metadata_value", String.class));
    }

    /**
     * Update a metadata entry.
     * 
     * @param fileId the file ID
     * @param key the metadata key
     * @param value the new value
     * @return true if updated successfully
     */
    @Transactional
    public boolean update(UUID fileId, String key, String value) {
        int updated = dsl.update(table(METADATA_TABLE))
                .set(field("metadata_value"), value)
                .where(field("file_id").eq(fileId))
                .and(field("metadata_key").eq(key))
                .execute();

        log.debug("Updated metadata for file {}: {} = {}", fileId, key, value);
        return updated > 0;
    }

    /**
     * Delete a metadata entry.
     * 
     * @param fileId the file ID
     * @param key the metadata key
     * @return true if deleted successfully
     */
    @Transactional
    public boolean delete(UUID fileId, String key) {
        int deleted = dsl.deleteFrom(table(METADATA_TABLE))
                .where(field("file_id").eq(fileId))
                .and(field("metadata_key").eq(key))
                .execute();

        log.debug("Deleted metadata for file {}: {}", fileId, key);
        return deleted > 0;
    }

    /**
     * Delete all metadata for a file.
     * 
     * @param fileId the file ID
     */
    @Transactional
    public void deleteByFileId(UUID fileId) {
        dsl.deleteFrom(table(METADATA_TABLE))
                .where(field("file_id").eq(fileId))
                .execute();

        log.debug("Deleted all metadata for file {}", fileId);
    }

    /**
     * Map a database record to a FileMetadataEntity.
     */
    private FileMetadataEntity mapToEntity(Record record) {
        return FileMetadataEntity.builder()
                .id(record.get(field("id", UUID.class)))
                .fileId(record.get(field("file_id", UUID.class)))
                .metadataKey(record.get(field("metadata_key", String.class)))
                .metadataValue(record.get(field("metadata_value", String.class)))
                .createdAt(record.get(field("created_at", LocalDateTime.class)))
                .build();
    }
}
