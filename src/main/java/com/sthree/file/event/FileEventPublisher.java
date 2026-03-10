package com.sthree.file.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.sthree.file.entity.FileEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publisher for file-related Kafka events.
 * 
 * Publishes events when files are uploaded, deleted, or shared
 * for consumption by other services (Chat, Profile, Group services).
 * Failed events are routed to dead-letter topics for later reprocessing.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileEventPublisher {

    private static final String DLT_SUFFIX = ".DLT";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.file-uploaded:file-uploaded}")
    private String fileUploadedTopic;

    @Value("${kafka.topics.file-deleted:file-deleted}")
    private String fileDeletedTopic;

    @Value("${kafka.topics.file-shared:file-shared}")
    private String fileSharedTopic;

    /**
     * Publish a file uploaded event.
     * 
     * @param file the uploaded file
     * @param userId the user who uploaded the file
     */
    public void publishFileUploaded(FileEntity file, UUID userId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "file.uploaded");
        event.put("fileId", file.getId().toString());
        event.put("fileName", file.getOriginalName());
        event.put("fileType", file.getFileType());
        event.put("fileSize", file.getFileSize());
        event.put("mimeType", file.getMimeType());
        event.put("uploadedBy", userId.toString());
        event.put("accessLevel", file.getAccessLevel() != null ? file.getAccessLevel().getValue() : null);
        event.put("timestamp", LocalDateTime.now().toString());

        sendWithDlt(fileUploadedTopic, file.getId().toString(), event);
    }

    /**
     * Publish a file deleted event.
     * 
     * @param fileId the deleted file ID
     * @param userId the user who deleted the file
     */
    public void publishFileDeleted(UUID fileId, UUID userId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "file.deleted");
        event.put("fileId", fileId.toString());
        event.put("deletedBy", userId.toString());
        event.put("timestamp", LocalDateTime.now().toString());

        sendWithDlt(fileDeletedTopic, fileId.toString(), event);
    }

    /**
     * Publish a file shared event.
     * 
     * @param fileId the shared file ID
     * @param userId the user who shared the file
     * @param accessLevel the access level
     */
    public void publishFileShared(UUID fileId, UUID userId, FileEntity.AccessLevel accessLevel) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "file.shared");
        event.put("fileId", fileId.toString());
        event.put("sharedBy", userId.toString());
        event.put("accessLevel", accessLevel.getValue());
        event.put("timestamp", LocalDateTime.now().toString());

        sendWithDlt(fileSharedTopic, fileId.toString(), event);
    }

    /**
     * Send an event to the given topic with dead-letter topic fallback.
     * On failure the event is forwarded to {@code <topic>.DLT} for reprocessing.
     */
    private void sendWithDlt(String topic, String key, Map<String, Object> event) {
        kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Published {} event to {}: key={}", event.get("eventType"), topic, key);
            } else {
                log.error("Failed to publish {} event to {}: key={}", event.get("eventType"), topic, key, ex);
                publishToDlt(topic, key, event);
            }
        });
    }

    /**
     * Forward a failed event to the dead-letter topic.
     */
    private void publishToDlt(String originalTopic, String key, Map<String, Object> event) {
        String dltTopic = originalTopic + DLT_SUFFIX;
        try {
            event.put("_originalTopic", originalTopic);
            event.put("_failedAt", LocalDateTime.now().toString());
            kafkaTemplate.send(dltTopic, key, event);
            log.warn("Routed failed event to DLT: {} -> {}", originalTopic, dltTopic);
        } catch (Exception dltEx) {
            log.error("Failed to send event to DLT {}: key={}", dltTopic, key, dltEx);
        }
    }
}
