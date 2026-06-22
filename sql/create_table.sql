# 数据库初始化

-- 创建库
create database if not exists zero_logic_db;

-- 切换库
use zero_logic_db;

-- 用户表
-- 以下是建表语句

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 应用表
create table app
(
    id           bigint auto_increment comment 'id' primary key,
    appName      varchar(256)                       null comment '应用名称',
    cover        varchar(512)                       null comment '应用封面',
    initPrompt   text                               null comment '应用初始化的 prompt',
    codeGenType  varchar(64)                        null comment '代码生成类型（枚举）',
    deployKey    varchar(64)                        null comment '部署标识',
    deployedTime datetime                           null comment '部署时间',
    priority     int      default 0                 not null comment '优先级',
    userId       bigint                             not null comment '创建用户id',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    initAttachmentId bigint null comment '初始化附件ID',
    UNIQUE KEY uk_deployKey (deployKey), -- 确保部署标识唯一
    INDEX idx_appName (appName),         -- 提升基于应用名称的查询性能
    INDEX idx_userId (userId)            -- 提升基于用户 ID 的查询性能
) comment '应用' collate = utf8mb4_unicode_ci;

-- 对话历史表
create table chat_history
(
    id          bigint auto_increment comment 'id' primary key,
    message     text                               not null comment '消息',
    messageType varchar(32)                        not null comment 'user/ai',
    appId       bigint                             not null comment '应用id',
    userId      bigint                             not null comment '创建用户id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    attachmentId bigint null comment '附件ID',
    INDEX idx_appId (appId),                       -- 提升基于应用的查询性能
    INDEX idx_createTime (createTime),             -- 提升基于时间的查询性能
    INDEX idx_appId_createTime (appId, createTime), -- 游标查询核心索引
    INDEX idx_attachmentId (attachmentId)
) comment '对话历史' collate = utf8mb4_unicode_ci;

CREATE TABLE prompt_attachment (
                                   id bigint AUTO_INCREMENT PRIMARY KEY,
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


CREATE TABLE generation_task (
         id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
         appId bigint NOT NULL COMMENT '应用 ID',
         userId bigint NOT NULL COMMENT '用户 ID',
         attachmentId bigint NULL COMMENT '本次生成使用的附件 ID',
         taskType varchar(32) NOT NULL DEFAULT 'generate' COMMENT '任务类型：generate/build/deploy/repair',
         status varchar(32) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/success/failed/canceled',
         currentStep varchar(64) NULL COMMENT '当前步骤',
         inputPrompt text NOT NULL COMMENT '用户原始输入',
         modelPrompt mediumtext NOT NULL COMMENT '实际发送给模型的 prompt',
         codeGenType varchar(64) NOT NULL COMMENT '代码生成类型',
         errorMessage text NULL COMMENT '失败原因',
         tokenUsage bigint NOT NULL DEFAULT 0 COMMENT 'token 消耗',
         toolCallCount int NOT NULL DEFAULT 0 COMMENT '工具调用次数',
         startTime datetime NULL COMMENT '开始时间',
         endTime datetime NULL COMMENT '结束时间',
         createTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
         updateTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
         isDelete tinyint NOT NULL DEFAULT 0,
         INDEX idx_appId (appId),
         INDEX idx_userId (userId),
         INDEX idx_status (status),
         INDEX idx_appId_createTime (appId, createTime),
         INDEX idx_userId_createTime (userId, createTime)
);

CREATE TABLE knowledge_document (
                                    id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                    appId bigint NOT NULL COMMENT '应用 ID',
                                    userId bigint NOT NULL COMMENT '用户 ID',
                                    attachmentId bigint NULL COMMENT '来源附件 ID',
                                    documentName varchar(256) NOT NULL COMMENT '文档名称',
                                    documentType varchar(64) NOT NULL DEFAULT 'attachment' COMMENT '文档类型',
                                    sourceType varchar(64) NOT NULL DEFAULT 'attachment' COMMENT '来源类型',
                                    contentHash varchar(64) NOT NULL COMMENT '内容 hash，防止重复入库',
                                    status varchar(32) NOT NULL DEFAULT 'active' COMMENT 'active/deleted',
                                    createTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updateTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    isDelete tinyint NOT NULL DEFAULT 0,
                                    UNIQUE KEY uk_app_attachment (appId, attachmentId),
                                    INDEX idx_appId (appId),
                                    INDEX idx_userId (userId),
                                    INDEX idx_contentHash (contentHash)
);

CREATE TABLE knowledge_chunk (
                                 id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                 documentId bigint NOT NULL COMMENT '文档 ID',
                                 appId bigint NOT NULL COMMENT '应用 ID',
                                 userId bigint NOT NULL COMMENT '用户 ID',
                                 chunkIndex int NOT NULL COMMENT '切片序号',
                                 content text NOT NULL COMMENT '切片内容',
                                 contentHash varchar(64) NOT NULL COMMENT '切片 hash',
                                 charLength int NOT NULL COMMENT '字符长度',
                                 createTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updateTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 isDelete tinyint NOT NULL DEFAULT 0,
                                 UNIQUE KEY uk_document_chunk (documentId, chunkIndex),
                                 INDEX idx_documentId (documentId),
                                 INDEX idx_appId (appId),
                                 INDEX idx_userId (userId)
);

CREATE TABLE knowledge_embedding (
                                     id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                     chunkId bigint NOT NULL COMMENT 'chunk ID',
                                     appId bigint NOT NULL COMMENT '应用 ID',
                                     userId bigint NOT NULL COMMENT '用户 ID',
                                     embeddingModel varchar(128) NOT NULL COMMENT 'embedding 模型',
                                     embeddingDimension int NOT NULL COMMENT '向量维度',
                                     embeddingJson mediumtext NOT NULL COMMENT '向量 JSON',
                                     createTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updateTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     isDelete tinyint NOT NULL DEFAULT 0,
                                     UNIQUE KEY uk_chunkId (chunkId),
                                     INDEX idx_appId (appId),
                                     INDEX idx_userId (userId)
);

CREATE TABLE rag_retrieval_log (
                                   id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                   taskId bigint NULL COMMENT '任务 ID',
                                   appId bigint NOT NULL COMMENT '应用 ID',
                                   userId bigint NOT NULL COMMENT '用户 ID',
                                   queryText text NOT NULL COMMENT '检索 query',
                                   topK int NOT NULL COMMENT 'TopK',
                                   hitCount int NOT NULL DEFAULT 0 COMMENT '命中数量',
                                   injectedCharLength int NOT NULL DEFAULT 0 COMMENT '注入上下文字符数',
                                   hitChunksJson mediumtext NULL COMMENT '命中 chunk JSON',
                                   createTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updateTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   isDelete tinyint NOT NULL DEFAULT 0,
                                   INDEX idx_taskId (taskId),
                                   INDEX idx_appId (appId),
                                   INDEX idx_userId (userId),
                                   INDEX idx_createTime (createTime)
);
