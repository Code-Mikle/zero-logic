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
create table if not exists app
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
create table if not exists chat_history
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
    taskId bigint null comment '关联生成任务 ID',
    INDEX idx_appId (appId),                       -- 提升基于应用的查询性能
    INDEX idx_createTime (createTime),             -- 提升基于时间的查询性能
    INDEX idx_appId_createTime (appId, createTime), -- 游标查询核心索引
    INDEX idx_attachmentId (attachmentId),
    INDEX idx_taskId (taskId)
) comment '对话历史' collate = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS prompt_attachment (
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


CREATE TABLE IF NOT EXISTS generation_task (
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

CREATE TABLE IF NOT EXISTS knowledge_document (
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

CREATE TABLE IF NOT EXISTS knowledge_chunk (
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

CREATE TABLE IF NOT EXISTS knowledge_embedding (
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

CREATE TABLE IF NOT EXISTS rag_retrieval_log (
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

CREATE TABLE IF NOT EXISTS generation_build_record (
    id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    taskId bigint NOT NULL COMMENT '生成任务 ID',
    appId bigint NOT NULL COMMENT '应用 ID',
    userId bigint NOT NULL COMMENT '用户 ID',
    attemptNo int NOT NULL DEFAULT 1 COMMENT '构建轮次',
    codeGenType varchar(64) NOT NULL COMMENT '代码生成类型',
    status varchar(32) NOT NULL COMMENT 'running/success/failed/timeout',
    command varchar(1024) NULL COMMENT '执行命令',
    exitCode int NULL COMMENT '进程退出码',
    logText mediumtext NULL COMMENT '构建日志',
    durationMs bigint NOT NULL DEFAULT 0 COMMENT '构建耗时（毫秒）',
    timedOut tinyint NOT NULL DEFAULT 0 COMMENT '是否超时',
    projectPath varchar(1024) NOT NULL COMMENT '项目目录',
    artifactPath varchar(1024) NULL COMMENT '构建产物目录',
    createTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    isDelete tinyint NOT NULL DEFAULT 0,
    UNIQUE KEY uk_task_attempt (taskId, attemptNo),
    INDEX idx_taskId (taskId),
    INDEX idx_appId (appId)
);

CREATE TABLE IF NOT EXISTS generation_repair_record (
    id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    taskId bigint NOT NULL,
    appId bigint NOT NULL,
    userId bigint NOT NULL,
    repairAttempt int NOT NULL,
    sourceBuildRecordId bigint NOT NULL,
    status varchar(32) NOT NULL,
    errorSummary text NULL,
    suspectedFiles text NULL,
    changedFiles text NULL,
    aiResponse text NULL,
    errorMessage varchar(2048) NULL,
    durationMs bigint NOT NULL DEFAULT 0,
    createTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    isDelete tinyint NOT NULL DEFAULT 0,
    UNIQUE KEY uk_task_repair_attempt (taskId, repairAttempt),
    INDEX idx_taskId (taskId),
    INDEX idx_appId (appId)
);


CREATE TABLE IF NOT EXISTS tool_call_record (
    id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    taskId bigint NULL COMMENT '生成任务 ID',
    appId bigint NOT NULL COMMENT '应用 ID',
    userId bigint NULL COMMENT '用户 ID',
    toolName varchar(128) NOT NULL COMMENT '工具名称',
    displayName varchar(128) NOT NULL COMMENT '工具展示名',
    toolCategory varchar(64) NOT NULL COMMENT '工具类别：file/build/deploy/knowledge/control',
    riskLevel varchar(32) NOT NULL COMMENT '风险等级：low/medium/high',
    callSource varchar(64) NULL COMMENT '调用来源：generate/repair/manual',
    status varchar(32) NOT NULL COMMENT 'success/failed/rejected',
    argumentsJson mediumtext NULL COMMENT '脱敏后的调用参数',
    resultSummary mediumtext NULL COMMENT '执行结果摘要',
    errorMessage varchar(2048) NULL COMMENT '错误信息',
    durationMs bigint NOT NULL DEFAULT 0 COMMENT '耗时',
    createTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    isDelete tinyint NOT NULL DEFAULT 0,
    INDEX idx_taskId (taskId),
    INDEX idx_appId (appId),
    INDEX idx_toolName (toolName),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime)
);

CREATE TABLE IF NOT EXISTS project_version (
    id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    appId bigint NOT NULL COMMENT '应用 ID',
    userId bigint NOT NULL COMMENT '用户 ID',
    taskId bigint NULL COMMENT '关联生成任务 ID',
    versionNo int NOT NULL COMMENT '应用内版本号',
    versionName varchar(64) NOT NULL COMMENT '版本名称',
    codeGenType varchar(64) NOT NULL COMMENT '代码生成类型',
    sourcePath varchar(1024) NOT NULL COMMENT '源码快照目录',
    artifactPath varchar(1024) NOT NULL COMMENT '可部署产物目录',
    buildRecordId bigint NULL COMMENT '关联构建记录 ID',
    status varchar(32) NOT NULL COMMENT 'created/built/failed/deployed',
    createTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    isDelete tinyint NOT NULL DEFAULT 0,
    UNIQUE KEY uk_app_version_no (appId, versionNo),
    INDEX idx_appId (appId),
    INDEX idx_userId (userId),
    INDEX idx_taskId (taskId),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime)
);

CREATE TABLE IF NOT EXISTS deploy_record (
    id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    appId bigint NOT NULL COMMENT '应用 ID',
    userId bigint NOT NULL COMMENT '用户 ID',
    versionId bigint NOT NULL COMMENT '项目版本 ID',
    deployKey varchar(64) NOT NULL COMMENT '部署标识',
    deployUrl varchar(1024) NULL COMMENT '部署访问地址',
    deployPath varchar(1024) NOT NULL COMMENT '部署目录',
    deployType varchar(32) NOT NULL DEFAULT 'deploy' COMMENT 'deploy/rollback',
    status varchar(32) NOT NULL COMMENT 'running/success/failed/rolled_back',
    errorMessage varchar(2048) NULL COMMENT '错误信息',
    createTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    isDelete tinyint NOT NULL DEFAULT 0,
    INDEX idx_appId (appId),
    INDEX idx_userId (userId),
    INDEX idx_versionId (versionId),
    INDEX idx_deployKey (deployKey),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime)
);

CREATE INDEX idx_priority_createTime ON app(priority, createTime);