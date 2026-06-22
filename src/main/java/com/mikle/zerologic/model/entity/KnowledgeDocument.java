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
@Table("knowledge_document")
public class KnowledgeDocument implements Serializable {

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
     * 来源附件 ID
     */
    @Column("attachmentId")
    private Long attachmentId;

    /**
     * 文档名称
     */
    @Column("documentName")
    private String documentName;

    /**
     * 文档类型
     */
    @Column("documentType")
    private String documentType;

    /**
     * 来源类型
     */
    @Column("sourceType")
    private String sourceType;

    /**
     * 内容 hash，防止重复入库
     */
    @Column("contentHash")
    private String contentHash;

    /**
     * active/deleted
     */
    private String status;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
