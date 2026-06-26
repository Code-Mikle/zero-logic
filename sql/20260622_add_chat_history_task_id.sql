SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'chat_history'
      AND column_name = 'taskId'
);
SET @sql := IF(@column_exists = 0,
    'ALTER TABLE chat_history ADD COLUMN taskId BIGINT NULL COMMENT ''关联生成任务 ID'' AFTER attachmentId',
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
      AND index_name = 'idx_taskId'
);
SET @sql := IF(@index_exists = 0,
    'ALTER TABLE chat_history ADD INDEX idx_taskId (taskId)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
