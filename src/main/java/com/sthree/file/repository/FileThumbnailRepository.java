package com.sthree.file.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sthree.file.entity.FileThumbnailEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.*;

/**
 * Repository for file thumbnail operations using JOOQ.
 * 
 * Provides data access methods for the file_thumbnails table,
 * managing thumbnail metadata for image files.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FileThumbnailRepository {

    private final DSLContext dsl;
    private static final String THUMBNAILS_TABLE = "file_thumbnails";

    /**
     * Insert a new thumbnail entry.
     * 
     * @param thumbnail the thumbnail entity to insert
     * @return the inserted thumbnail entity
     */
    @Transactional
    public FileThumbnailEntity insert(FileThumbnailEntity thumbnail) {
        if (thumbnail.getId() == null) {
            thumbnail.setId(UUID.randomUUID());
        }
        if (thumbnail.getCreatedAt() == null) {
            thumbnail.setCreatedAt(LocalDateTime.now());
        }

        dsl.insertInto(table(THUMBNAILS_TABLE))
                .set(field("id"), thumbnail.getId())
                .set(field("file_id"), thumbnail.getFileId())
                .set(field("thumbnail_path"), thumbnail.getThumbnailPath())
                .set(field("thumbnail_size"), thumbnail.getThumbnailSize().getValue())
                .set(field("width"), thumbnail.getWidth())
                .set(field("height"), thumbnail.getHeight())
                .set(field("created_at"), thumbnail.getCreatedAt())
                .onConflict(field("file_id"), field("thumbnail_size"))
                .doUpdate()
                .set(field("thumbnail_path"), thumbnail.getThumbnailPath())
                .set(field("width"), thumbnail.getWidth())
                .set(field("height"), thumbnail.getHeight())
                .execute();

        log.debug("Inserted thumbnail for file {}: {}x{}", 
                thumbnail.getFileId(), thumbnail.getWidth(), thumbnail.getHeight());
        return thumbnail;
    }

    /**
     * Find all thumbnails for a file.
     * 
     * @param fileId the file ID
     * @return list of thumbnails for the file
     */
    public List<FileThumbnailEntity> findByFileId(UUID fileId) {
        Result<Record> records = dsl.selectFrom(table(THUMBNAILS_TABLE))
                .where(field("file_id").eq(fileId))
                .fetch();

        return records.map(this::mapToEntity);
    }

    /**
     * Find a specific thumbnail for a file.
     * 
     * @param fileId the file ID
     * @param size the thumbnail size
     * @return Optional containing the thumbnail if found
     */
    public Optional<FileThumbnailEntity> findByFileIdAndSize(
            UUID fileId, FileThumbnailEntity.ThumbnailSize size) {
        
        Record record = dsl.selectFrom(table(THUMBNAILS_TABLE))
                .where(field("file_id").eq(fileId))
                .and(field("thumbnail_size").eq(size.getValue()))
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToEntity);
    }

    /**
     * Find a thumbnail by ID.
     * 
     * @param id the thumbnail ID
     * @return Optional containing the thumbnail if found
     */
    public Optional<FileThumbnailEntity> findById(UUID id) {
        Record record = dsl.selectFrom(table(THUMBNAILS_TABLE))
                .where(field("id").eq(id))
                .fetchOne();

        return Optional.ofNullable(record).map(this::mapToEntity);
    }

    /**
     * Delete a thumbnail.
     * 
     * @param fileId the file ID
     * @param size the thumbnail size
     * @return true if deleted successfully
     */
    @Transactional
    public boolean delete(UUID fileId, FileThumbnailEntity.ThumbnailSize size) {
        int deleted = dsl.deleteFrom(table(THUMBNAILS_TABLE))
                .where(field("file_id").eq(fileId))
                .and(field("thumbnail_size").eq(size.getValue()))
                .execute();

        log.debug("Deleted thumbnail for file {}: {}", fileId, size);
        return deleted > 0;
    }

    /**
     * Delete all thumbnails for a file.
     * 
     * @param fileId the file ID
     */
    @Transactional
    public void deleteByFileId(UUID fileId) {
        dsl.deleteFrom(table(THUMBNAILS_TABLE))
                .where(field("file_id").eq(fileId))
                .execute();

        log.debug("Deleted all thumbnails for file {}", fileId);
    }

    /**
     * Check if a file has thumbnails.
     * 
     * @param fileId the file ID
     * @return true if the file has thumbnails
     */
    public boolean hasThumbnails(UUID fileId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(table(THUMBNAILS_TABLE))
                        .where(field("file_id").eq(fileId))
        );
    }

    /**
     * Count thumbnails for a file.
     * 
     * @param fileId the file ID
     * @return number of thumbnails
     */
    public int countByFileId(UUID fileId) {
        Long count = dsl.selectCount()
                .from(table(THUMBNAILS_TABLE))
                .where(field("file_id").eq(fileId))
                .fetchOne(0, Long.class);

        return count != null ? count.intValue() : 0;
    }

    /**
     * Map a database record to a FileThumbnailEntity.
     */
    private FileThumbnailEntity mapToEntity(Record record) {
        String sizeStr = record.get(field("thumbnail_size", String.class));

        return FileThumbnailEntity.builder()
                .id(record.get(field("id", UUID.class)))
                .fileId(record.get(field("file_id", UUID.class)))
                .thumbnailPath(record.get(field("thumbnail_path", String.class)))
                .thumbnailSize(FileThumbnailEntity.ThumbnailSize.fromString(sizeStr))
                .width(record.get(field("width", Integer.class)))
                .height(record.get(field("height", Integer.class)))
                .createdAt(record.get(field("created_at", LocalDateTime.class)))
                .build();
    }
}
