package com.mikle.zerologic.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class DeployRecordVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long appId;
    private Long versionId;
    private String deployKey;
    private String deployUrl;
    private String deployType;
    private String status;
    private String errorMessage;
    private LocalDateTime createTime;
}
