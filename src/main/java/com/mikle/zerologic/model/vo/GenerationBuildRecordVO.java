package com.mikle.zerologic.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class GenerationBuildRecordVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer attemptNo;
    private String status;
    private String command;
    private Integer exitCode;
    private String logText;
    private Long durationMs;
    private Boolean timedOut;
    private String artifactPath;
    private LocalDateTime createTime;
}
