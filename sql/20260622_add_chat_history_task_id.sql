ALTER TABLE chat_history
    ADD COLUMN taskId BIGINT NULL COMMENT '关联生成任务 ID' AFTER attachmentId,
    ADD INDEX idx_taskId (taskId);
