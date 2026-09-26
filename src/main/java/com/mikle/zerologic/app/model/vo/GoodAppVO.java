package com.mikle.zerologic.app.model.vo;

import com.mikle.zerologic.user.model.vo.UserVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 首页精选应用展示对象。
 */
@Data
public class GoodAppVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String appName;

    private String cover;

    private String codeGenType;

    private String deployKey;

    private UserVO user;
}
