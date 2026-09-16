package com.mikle.zerologic.knowledge.retrieval.service;

import com.mikle.zerologic.knowledge.retrieval.model.result.RagRetrievedChunk;

import java.util.List;

/**
 * 检索 top k
 */
public interface VectorSearchService {

    List<RagRetrievedChunk> search(Long appId, Long userId, Long attachmentId,
                                   String query, Integer topK);
}
