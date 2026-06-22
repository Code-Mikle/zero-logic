package com.mikle.zerologic.service;

import com.mikle.zerologic.model.entity.User;

/**
 * 把附件导入知识库
 */
public interface KnowledgeIngestService {

    Long ingestAttachment(Long attachmentId, Long appId, User loginUser);
}
