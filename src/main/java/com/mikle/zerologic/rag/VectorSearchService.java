package com.mikle.zerologic.rag;

import com.mikle.zerologic.rag.model.RagRetrievedChunk;

import java.util.List;

/**
 * 检索 top k
 */
public interface VectorSearchService {

    List<RagRetrievedChunk> search(Long appId, Long userId, Long attachmentId,
                                   String query, Integer topK);
}
