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
@Table("rag_retrieval_log")
public class RagRetrievalLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 任务 ID
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
     * 检索 query
     */
    @Column("queryText")
    private String queryText;

    /**
     * TopK
     */
    @Column("topK")
    private Integer topK;

    /**
     * 命中数量
     */
    @Column("hitCount")
    private Integer hitCount;

    /**
     * 注入上下文字符数
     */
    @Column("injectedCharLength")
    private Integer injectedCharLength;

    /**
     * 命中 chunk JSON
     */
    @Column("hitChunksJson")
    private String hitChunksJson;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
