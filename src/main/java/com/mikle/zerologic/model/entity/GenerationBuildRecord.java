package com.mikle.zerologic.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("generation_build_record")
public class GenerationBuildRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("taskId")
    private Long taskId;
    @Column("appId")
    private Long appId;
    @Column("userId")
    private Long userId;
    @Column("attemptNo")
    private Integer attemptNo;
    @Column("codeGenType")
    private String codeGenType;
    private String status;
    private String command;
    @Column("exitCode")
    private Integer exitCode;
    @Column("logText")
    private String logText;
    @Column("durationMs")
    private Long durationMs;
    @Column("timedOut")
    private Boolean timedOut;
    @Column("projectPath")
    private String projectPath;
    @Column("artifactPath")
    private String artifactPath;
    @Column("createTime")
    private LocalDateTime createTime;
    @Column("updateTime")
    private LocalDateTime updateTime;
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
