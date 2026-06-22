package com.mikle.zerologic.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RagRetrievalVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long taskId;

    private String queryText;

    private Integer topK;

    private Integer hitCount;

    private Integer injectedCharLength;

    private List<RagReferenceVO> references;

    private LocalDateTime createTime;

}
