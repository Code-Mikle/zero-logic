package com.mikle.zerologic.model.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 当前登录用户资料更新请求。
 */
@Data
public class UserProfileUpdateRequest implements Serializable {

    /**
     * 用户昵称
     */
    @Size(max = 80, message = "用户昵称不能超过 80 个字符")
    private String userName;

    /**
     * 用户头像
     */
    @Size(max = 1024, message = "头像地址不能超过 1024 个字符")
    private String userAvatar;

    /**
     * 用户简介
     */
    @Size(max = 512, message = "个人简介不能超过 512 个字符")
    private String userProfile;

    private static final long serialVersionUID = 1L;
}
