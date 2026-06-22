CREATE TABLE prompt_attachment (
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

ALTER TABLE app ADD COLUMN initAttachmentId bigint NULL COMMENT '初始化附件ID';
ALTER TABLE chat_history ADD COLUMN attachmentId bigint NULL COMMENT '附件ID';
ALTER TABLE chat_history ADD INDEX idx_attachmentId (attachmentId);
