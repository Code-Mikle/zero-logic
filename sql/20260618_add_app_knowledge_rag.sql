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
