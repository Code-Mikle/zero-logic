package com.mikle.zerologic.knowledge.retrieval.model.result;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * RAG 节点返回给工作流的结果
 */
@Data
@Builder
public class RagResult {

    private String contextText;

    private List<RagRetrievedChunk> references;

    public boolean hasContext() {
        return contextText != null && !contextText.isBlank();
    }
}