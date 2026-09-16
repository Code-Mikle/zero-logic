package com.mikle.zerologic.knowledge.retrieval.service;

import com.mikle.zerologic.knowledge.retrieval.model.result.RagResult;

public interface RagService {

    RagResult retrieve(Long taskId, Long appId, Long userId, Long attachmentId, String query);
}
