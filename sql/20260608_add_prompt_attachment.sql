CREATE TABLE IF NOT EXISTS prompt_attachment (
                                   id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                   fileName varchar(256) NOT NULL,
                                   fileExtension varchar(32) NOT NULL,
                                   contentType varchar(128) NULL,
                                   fileSize bigint NOT NULL,
                                   content mediumtext NOT NULL,
                                   userId bigint NOT NULL,
                                   appId bigint NULL,
                                   status varchar(32) NOT NULL DEFAULT 'temporary',
                                   createTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updateTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   isDelete tinyint NOT NULL DEFAULT 0,
                                   INDEX idx_userId (userId),
                                   INDEX idx_appId (appId),
                                   INDEX idx_status_createTime (status, createTime)
);

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'app'
      AND column_name = 'initAttachmentId'
);
SET @sql := IF(@column_exists = 0,
    'ALTER TABLE app ADD COLUMN initAttachmentId bigint NULL COMMENT ''初始化附件ID''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'chat_history'
      AND column_name = 'attachmentId'
);
SET @sql := IF(@column_exists = 0,
    'ALTER TABLE chat_history ADD COLUMN attachmentId bigint NULL COMMENT ''附件ID''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'chat_history'
      AND index_name = 'idx_attachmentId'
);
SET @sql := IF(@index_exists = 0,
    'ALTER TABLE chat_history ADD INDEX idx_attachmentId (attachmentId)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
