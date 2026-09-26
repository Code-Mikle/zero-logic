package com.mikle.zerologic.app.model.dto;

import com.mikle.zerologic.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前用户应用分页查询请求。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MyAppQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用名称。
     */
    private String appName;

    /**
     * 代码生成类型。
     */
    private String codeGenType;
}
