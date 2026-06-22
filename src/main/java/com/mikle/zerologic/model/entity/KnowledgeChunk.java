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
@Table("knowledge_chunk")
public class KnowledgeChunk implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 文档 ID
     */
    @Column("documentId")
    private Long documentId;

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
     * 切片序号
     */
    @Column("chunkIndex")
    private Integer chunkIndex;

    /**
     * 切片内容
     */
    private String content;

    /**
     * 切片 hash
     */
    @Column("contentHash")
    private String contentHash;

    /**
     * 字符长度
     */
    @Column("charLength")
    private Integer charLength;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
