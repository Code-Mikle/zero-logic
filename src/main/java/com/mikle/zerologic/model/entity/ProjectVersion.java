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
@Table("project_version")
public class ProjectVersion implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("appId")
    private Long appId;
    @Column("userId")
    private Long userId;
    @Column("taskId")
    private Long taskId;
    @Column("versionNo")
    private Integer versionNo;
    @Column("versionName")
    private String versionName;
    @Column("codeGenType")
    private String codeGenType;
    @Column("sourcePath")
    private String sourcePath;
    @Column("artifactPath")
    private String artifactPath;
    @Column("buildRecordId")
    private Long buildRecordId;
    private String status;
    @Column("createTime")
    private LocalDateTime createTime;
    @Column("updateTime")
    private LocalDateTime updateTime;
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
