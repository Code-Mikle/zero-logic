package com.mikle.zerologic.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class GenerationTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long appId;

    private Long userId;

    private Long attachmentId;

    private String taskType;

    private String status;

    private String currentStep;

    private String inputPrompt;

    private String codeGenType;

    private String errorMessage;

    private Long tokenUsage;

    private Integer toolCallCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;

}
