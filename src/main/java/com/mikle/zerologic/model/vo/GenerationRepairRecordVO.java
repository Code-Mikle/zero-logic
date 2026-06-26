package com.mikle.zerologic.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GenerationRepairRecordVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Integer repairAttempt;
    private Long sourceBuildRecordId;
    private String status;
    private String errorSummary;
    private List<String> suspectedFiles;
    private List<String> changedFiles;
    private String aiResponse;
    private String errorMessage;
    private Long durationMs;
    private LocalDateTime createTime;
}
