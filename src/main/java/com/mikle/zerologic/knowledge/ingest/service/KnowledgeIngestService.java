package com.mikle.zerologic.knowledge.ingest.service;

import com.mikle.zerologic.user.model.entity.User;

/**
 * 把附件导入知识库
 */
public interface KnowledgeIngestService {

    Long ingestAttachment(Long attachmentId, Long appId, User loginUser);
}
