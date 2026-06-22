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
@Table("prompt_attachment")
public class PromptAttachment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("fileName")
    private String fileName;

    /**
     * 文件拓展名
     */
    @Column("fileExtension")
    private String fileExtension;

    /**
     * MIME 类型
     */
    @Column("contentType")
    private String contentType;

    /**
     * 文件大小，单位字节
     */
    @Column("fileSize")
    private Long fileSize;

    /**
     * 文件解析后的文本
     */
    private String content;

    @Column("userId")
    private Long userId;

    @Column("appId")
    private Long appId;

    /**
     * temporary 或 bound
     */
    private String status;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
