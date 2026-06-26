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
@Table("generation_repair_record")
public class GenerationRepairRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("taskId") private Long taskId;
    @Column("appId") private Long appId;
    @Column("userId") private Long userId;
    @Column("repairAttempt") private Integer repairAttempt;
    @Column("sourceBuildRecordId") private Long sourceBuildRecordId;
    private String status;
    @Column("errorSummary") private String errorSummary;
    @Column("suspectedFiles") private String suspectedFiles;
    @Column("changedFiles") private String changedFiles;
    @Column("aiResponse") private String aiResponse;
    @Column("errorMessage") private String errorMessage;
    @Column("durationMs") private Long durationMs;
    @Column("createTime") private LocalDateTime createTime;
    @Column("updateTime") private LocalDateTime updateTime;
    @Column(value = "isDelete", isLogicDelete = true) private Integer isDelete;
}
