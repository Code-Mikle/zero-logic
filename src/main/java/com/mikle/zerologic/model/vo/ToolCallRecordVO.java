package com.mikle.zerologic.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ToolCallRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String toolName;

    private String displayName;

    private String toolCategory;

    private String riskLevel;

    private String callSource;

    private String status;

    private String argumentsJson;

    private String resultSummary;

    private String errorMessage;

    private Long durationMs;

    private LocalDateTime createTime;
}
