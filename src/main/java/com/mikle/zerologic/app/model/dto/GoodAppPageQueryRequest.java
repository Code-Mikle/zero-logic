package com.mikle.zerologic.app.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 精选应用分页查询请求。
 *
 * 精选条件和排序规则由服务端固定，避免客户端构造无效查询和缓存 key。
 */
@Data
public class GoodAppPageQueryRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int pageNum = 1;

    private int pageSize = 10;
}
