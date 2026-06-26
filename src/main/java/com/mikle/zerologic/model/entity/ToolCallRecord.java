package com.mikle.zerologic.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *  实体类。
 *
 * @author <a href="https://github.com/Code-Mikle">Mikle</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("tool_call_record")
public class ToolCallRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 生成任务 ID
     */
    @Column("taskId")
    private Long taskId;

    /**
     * 应用 ID
     */
    @Column("appId")
    private Long appId;

    /**
     * 用户 ID
     */
    @Column("userId")
    private Long userId;

    /**
     * 工具名称
     */
    @Column("toolName")
    private String toolName;

    /**
     * 工具展示名
     */
    @Column("displayName")
    private String displayName;

    /**
     * 工具类别：file/build/deploy/knowledge/control
     */
    @Column("toolCategory")
    private String toolCategory;

    /**
     * 风险等级：low/medium/high
     */
    @Column("riskLevel")
    private String riskLevel;

    /**
     * 调用来源：generate/repair/manual
     */
    @Column("callSource")
    private String callSource;

    /**
     * success/failed/rejected
     */
    private String status;

    /**
     * 脱敏后的调用参数
     */
    @Column("argumentsJson")
    private String argumentsJson;

    /**
     * 执行结果摘要
     */
    @Column("resultSummary")
    private String resultSummary;

    /**
     * 错误信息
     */
    @Column("errorMessage")
    private String errorMessage;

    /**
     * 耗时
     */
    @Column("durationMs")
    private Long durationMs;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
