package com.mikle.zerologic.knowledge.ingest.model;

import lombok.Builder;
import lombok.Data;

/**
 * 切片过程的内存对象，不是数据库实体
 */
@Data
@Builder
public class DocumentChunk {

    private Integer chunkIndex;

    private String content;

    private String contentHash;

    private Integer charLength;
}