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
@Table("deploy_record")
public class DeployRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("appId")
    private Long appId;
    @Column("userId")
    private Long userId;
    @Column("versionId")
    private Long versionId;
    @Column("deployKey")
    private String deployKey;
    @Column("deployUrl")
    private String deployUrl;
    @Column("deployPath")
    private String deployPath;
    @Column("deployType")
    private String deployType;
    private String status;
    @Column("errorMessage")
    private String errorMessage;
    @Column("createTime")
    private LocalDateTime createTime;
    @Column("updateTime")
    private LocalDateTime updateTime;
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
