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
@Table("generation_task")
public class GenerationTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

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
     * 本次生成使用的附件 ID
     */
    @Column("attachmentId")
    private Long attachmentId;

    /**
     * 任务类型：generate/build/deploy/repair
     */
    @Column("taskType")
    private String taskType;

    /**
     * pending/running/success/failed/canceled
     */
    private String status;

    /**
     * 当前步骤
     */
    @Column("currentStep")
    private String currentStep;

    /**
     * 用户原始输入
     */
    @Column("inputPrompt")
    private String inputPrompt;

    /**
     * 实际发送给模型的 prompt
     */
    @Column("modelPrompt")
    private String modelPrompt;

    /**
     * 代码生成类型
     */
    @Column("codeGenType")
    private String codeGenType;

    /**
     * 失败原因
     */
    @Column("errorMessage")
    private String errorMessage;

    /**
     * token 消耗
     */
    @Column("tokenUsage")
    private Long tokenUsage;

    /**
     * 工具调用次数
     */
    @Column("toolCallCount")
    private Integer toolCallCount;

    /**
     * 开始时间
     */
    @Column("startTime")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Column("endTime")
    private LocalDateTime endTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
