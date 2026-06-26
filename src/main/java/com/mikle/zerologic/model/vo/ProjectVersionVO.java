package com.mikle.zerologic.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ProjectVersionVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long appId;
    private Long taskId;
    private Integer versionNo;
    private String versionName;
    private String codeGenType;
    private String status;
    private Long buildRecordId;
    private LocalDateTime createTime;
}
