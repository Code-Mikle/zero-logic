package com.mikle.zerologic.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用创建请求
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 应用初始化时附带的附件
     */
    private Long attachmentId;

    private static final long serialVersionUID = 1L;
} 