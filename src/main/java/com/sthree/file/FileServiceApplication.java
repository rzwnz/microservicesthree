package com.sthree.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Application Entry Point for File Service.
 * 
 * The File Service handles file uploads, storage, and retrieval for the platform.
 * It manages images (profile pictures, chat images), code snippets, and other file types.
 * Files are stored in Garage object storage (S3-compatible) with proper access control,
 * versioning, and metadata management.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class FileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileServiceApplication.class, args);
    }
}
