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
@Table("knowledge_embedding")
public class KnowledgeEmbedding implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * chunk ID
     */
    @Column("chunkId")
    private Long chunkId;

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
     * embedding 模型
     */
    @Column("embeddingModel")
    private String embeddingModel;

    /**
     * 向量维度
     */
    @Column("embeddingDimension")
    private Integer embeddingDimension;

    /**
     * 向量 JSON
     */
    @Column("embeddingJson")
    private String embeddingJson;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
