package com.mikle.zerologic.rag.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 检索结果
 */
@Data
@Builder
public class RagRetrievedChunk implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long documentId;

    private Long chunkId;

    private String documentName;

    private Integer chunkIndex;

    private String content;

    private Double score;
}
