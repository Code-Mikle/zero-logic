package com.mikle.zerologic.model.vo;


import lombok.Data;

import java.io.Serializable;

@Data
public class RagReferenceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long documentId;

    private Long chunkId;

    private String documentName;

    private Integer chunkIndex;

    /** 返回前端的内容摘要，服务层最多保留 400 字。 */
    private String contentSnippet;

    private Double score;
}
