-- ===========================================
-- File Service Database Schema
-- ===========================================
-- PostgreSQL schema for the File Service.
-- Uses UUID primary keys for distributed compatibility.
-- ===========================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ===========================================
-- Files Table
-- ===========================================
-- Stores metadata for all uploaded files.
-- Supports soft delete with deleted_at column.
CREATE TABLE IF NOT EXISTS files (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL, -- 'image', 'code', 'document'
    mime_type VARCHAR(100) NOT NULL,
    storage_path TEXT NOT NULL, -- Path in Garage/S3
    bucket_name VARCHAR(100) NOT NULL,
    uploaded_by UUID NOT NULL, -- References Auth Service user
    access_level VARCHAR(20) DEFAULT 'private', -- 'public', 'private', 'shared'
    checksum VARCHAR(64), -- SHA-256 checksum
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP -- Soft delete
);

-- Indexes for files table
CREATE INDEX IF NOT EXISTS idx_files_uploaded_by ON files(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_files_file_type ON files(file_type);
CREATE INDEX IF NOT EXISTS idx_files_created_at ON files(created_at);
CREATE INDEX IF NOT EXISTS idx_files_deleted_at ON files(deleted_at);
CREATE INDEX IF NOT EXISTS idx_files_access_level ON files(access_level);

COMMENT ON TABLE files IS 'Stores metadata for all uploaded files';
COMMENT ON COLUMN files.storage_path IS 'Path to file in Garage object storage';
COMMENT ON COLUMN files.bucket_name IS 'Garage bucket name where file is stored';
COMMENT ON COLUMN files.access_level IS 'Access level: public, private, or shared';

-- ===========================================
-- File Metadata Table
-- ===========================================
-- Stores key-value metadata for files.
-- Supports custom metadata like dimensions, description, tags.
CREATE TABLE IF NOT EXISTS file_metadata (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_file_metadata_file_key UNIQUE (file_id, metadata_key)
);

-- Index for file_metadata table
CREATE INDEX IF NOT EXISTS idx_file_metadata_file_id ON file_metadata(file_id);
CREATE INDEX IF NOT EXISTS idx_file_metadata_key ON file_metadata(metadata_key);

COMMENT ON TABLE file_metadata IS 'Key-value metadata storage for files';
COMMENT ON COLUMN file_metadata.metadata_key IS 'Metadata key: width, height, description, tags, etc.';

-- ===========================================
-- File Access Table
-- ===========================================
-- Manages access control for shared files.
-- Supports read, write, delete access types.
CREATE TABLE IF NOT EXISTS file_access (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    user_id UUID, -- References Auth Service user (NULL for public)
    access_type VARCHAR(20) NOT NULL, -- 'read', 'write', 'delete'
    granted_by UUID, -- User who granted access
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP, -- Optional expiration
    CONSTRAINT uq_file_access_unique UNIQUE (file_id, user_id, access_type)
);

-- Indexes for file_access table
CREATE INDEX IF NOT EXISTS idx_file_access_file_id ON file_access(file_id);
CREATE INDEX IF NOT EXISTS idx_file_access_user_id ON file_access(user_id);
CREATE INDEX IF NOT EXISTS idx_file_access_expires_at ON file_access(expires_at);

COMMENT ON TABLE file_access IS 'Access control entries for shared files';
COMMENT ON COLUMN file_access.access_type IS 'Type of access: read, write, or delete';

-- ===========================================
-- File Shares Table
-- ===========================================
-- Manages shareable links for files.
-- Supports password protection and download limits.
CREATE TABLE IF NOT EXISTS file_shares (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    share_token VARCHAR(255) UNIQUE NOT NULL,
    shared_by UUID NOT NULL, -- References Auth Service user
    access_level VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255), -- Optional password protection
    expires_at TIMESTAMP, -- Optional expiration
    max_downloads INTEGER, -- Optional download limit
    download_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for file_shares table
CREATE INDEX IF NOT EXISTS idx_file_shares_share_token ON file_shares(share_token);
CREATE INDEX IF NOT EXISTS idx_file_shares_file_id ON file_shares(file_id);
CREATE INDEX IF NOT EXISTS idx_file_shares_shared_by ON file_shares(shared_by);
CREATE INDEX IF NOT EXISTS idx_file_shares_expires_at ON file_shares(expires_at);

COMMENT ON TABLE file_shares IS 'Shareable links for files with optional protection';
COMMENT ON COLUMN file_shares.share_token IS 'Unique token for the share URL';
COMMENT ON COLUMN file_shares.password_hash IS 'BCrypt hash of optional password';

-- ===========================================
-- File Thumbnails Table
-- ===========================================
-- Stores thumbnail metadata for images.
-- Supports multiple thumbnail sizes.
CREATE TABLE IF NOT EXISTS file_thumbnails (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    thumbnail_path TEXT NOT NULL,
    thumbnail_size VARCHAR(20) NOT NULL, -- 'small', 'medium', 'large'
    width INTEGER,
    height INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_file_thumbnails_file_size UNIQUE (file_id, thumbnail_size)
);

-- Index for file_thumbnails table
CREATE INDEX IF NOT EXISTS idx_file_thumbnails_file_id ON file_thumbnails(file_id);

COMMENT ON TABLE file_thumbnails IS 'Generated thumbnails for image files';
COMMENT ON COLUMN file_thumbnails.thumbnail_size IS 'Size category: small (150x150), medium (300x300), large (600x600)';

-- ===========================================
-- Functions
-- ===========================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to auto-update updated_at
DROP TRIGGER IF EXISTS update_files_updated_at ON files;
CREATE TRIGGER update_files_updated_at
    BEFORE UPDATE ON files
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ===========================================
-- Cleanup Functions
-- ===========================================

-- Function to clean up expired access entries
CREATE OR REPLACE FUNCTION cleanup_expired_access()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM file_access
    WHERE expires_at IS NOT NULL AND expires_at < CURRENT_TIMESTAMP;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to clean up expired shares
CREATE OR REPLACE FUNCTION cleanup_expired_shares()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM file_shares
    WHERE expires_at IS NOT NULL AND expires_at < CURRENT_TIMESTAMP;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to get files ready for permanent deletion
CREATE OR REPLACE FUNCTION get_files_for_cleanup(retention_days INTEGER DEFAULT 30)
RETURNS TABLE (
    file_id UUID,
    storage_path TEXT,
    bucket_name VARCHAR
) AS $$
BEGIN
    RETURN QUERY
    SELECT f.id, f.storage_path, f.bucket_name
    FROM files f
    WHERE f.deleted_at IS NOT NULL
    AND f.deleted_at < CURRENT_TIMESTAMP - (retention_days || ' days')::INTERVAL;
END;
$$ LANGUAGE plpgsql;

-- ===========================================
-- Initial Data (Optional)
-- ===========================================

-- Insert default buckets info (for reference)
-- This is handled by application configuration, not database

-- ===========================================
-- Grant Permissions
-- ===========================================

-- Replace 'file_service_user' with your database user
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO file_service_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO file_service_user;
-- GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO file_service_user;
