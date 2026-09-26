package com.mikle.zerologic.user.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.mikle.zerologic.user.model.dto.UserProfileUpdateRequest;
import com.mikle.zerologic.user.model.dto.UserQueryRequest;
import com.mikle.zerologic.user.model.entity.User;
import com.mikle.zerologic.user.model.vo.LoginUserVO;
import com.mikle.zerologic.user.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 * @author <a href="https://github.com/Code-Mikle">Mikle</a>
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取脱敏的已登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户登录
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 更新当前登录用户资料
     */
    User updateMyUserProfile(UserProfileUpdateRequest request, User loginUser);

    /**
     * 获取当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取脱敏后的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息（分页）
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户注销
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 根据查询条件构造数据查询参数
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 加密
     */
    String getEncryptPassword(String userPassword);
}
