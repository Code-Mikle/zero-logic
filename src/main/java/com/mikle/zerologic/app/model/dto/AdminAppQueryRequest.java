package com.mikle.zerologic.app.model.dto;

import com.mikle.zerologic.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理员应用分页查询请求。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AdminAppQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 应用 ID。 */
    private Long id;

    /** 应用名称。 */
    private String appName;

    /** 代码生成类型。 */
    private String codeGenType;

    /** 部署标识。 */
    private String deployKey;

    /** 优先级。 */
    private Integer priority;

    /** 创建用户 ID。 */
    private Long userId;
}
