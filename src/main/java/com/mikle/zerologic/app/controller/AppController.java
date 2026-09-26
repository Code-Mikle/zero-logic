package com.mikle.zerologic.app.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mikle.zerologic.app.deployment.model.dto.AppDeployRequest;
import com.mikle.zerologic.app.deployment.model.dto.AppVersionDeployRequest;
import com.mikle.zerologic.app.model.dto.AdminAppQueryRequest;
import com.mikle.zerologic.app.model.dto.AppAddRequest;
import com.mikle.zerologic.app.model.dto.AppAdminUpdateRequest;
import com.mikle.zerologic.app.model.dto.AppUpdateRequest;
import com.mikle.zerologic.app.model.dto.GoodAppPageQueryRequest;
import com.mikle.zerologic.app.model.dto.MyAppQueryRequest;
import com.mybatisflex.core.paginate.Page;
import com.mikle.zerologic.user.auth.AuthCheck;
import com.mikle.zerologic.common.BaseResponse;
import com.mikle.zerologic.common.DeleteRequest;
import com.mikle.zerologic.common.ResultUtils;
import com.mikle.zerologic.app.constant.AppConstant;
import com.mikle.zerologic.user.constant.UserConstant;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.exception.ThrowUtils;
import com.mikle.zerologic.user.model.entity.User;
import com.mikle.zerologic.app.model.vo.AppVO;
import com.mikle.zerologic.app.model.vo.GoodAppVO;
import com.mikle.zerologic.app.deployment.model.vo.DeployRecordVO;
import com.mikle.zerologic.app.version.model.vo.ProjectVersionVO;
import com.mikle.zerologic.app.download.service.ProjectDownloadService;
import com.mikle.zerologic.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;
import com.mikle.zerologic.app.model.entity.App;
import com.mikle.zerologic.app.service.AppService;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用 控制层。
 * @author <a href="https://github.com/Code-Mikle">Mikle</a>
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private ProjectDownloadService projectDownloadService;

    /**
     * 创建应用
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        Long appId = appService.createApp(appAddRequest, loginUser);
        return ResultUtils.success(appId);
    }

    /**
     * 删除应用（用户只能删除自己的应用）
     */
    @PostMapping("/delete")
    @CacheEvict(value = AppConstant.GOOD_APP_CACHE_NAME, allEntries = true)
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldApp.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 管理员删除应用
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @CacheEvict(value = AppConstant.GOOD_APP_CACHE_NAME, allEntries = true)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新应用（用户只能更新自己的应用名称）
     */
    @PostMapping("/update")
    @CacheEvict(value = AppConstant.GOOD_APP_CACHE_NAME, allEntries = true)
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = appUpdateRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人可更新
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App app = new App();
        app.setId(id);
        app.setAppName(appUpdateRequest.getAppName());
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员更新应用
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @CacheEvict(value = AppConstant.GOOD_APP_CACHE_NAME, allEntries = true)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = appAdminUpdateRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类（包含用户信息）
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页获取当前用户创建的应用列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody MyAppQueryRequest myAppQueryRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(myAppQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(appService.pageMyApps(myAppQueryRequest, loginUser.getId()));
    }

    /**
     * 管理员分页获取应用列表
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(
            @RequestBody AdminAppQueryRequest adminAppQueryRequest) {
        ThrowUtils.throwIf(adminAppQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.pageAdminApps(adminAppQueryRequest));
    }

    /**
     * 分页获取精选应用列表
     */
    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<GoodAppVO>> listGoodAppVOByPage(
            @RequestBody GoodAppPageQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(appService.pageGoodApps(queryRequest));
    }

    /**
     * 应用部署
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        // 检查部署请求是否为空
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取应用 ID
        Long appId = appDeployRequest.getAppId();
        // 检查应用 ID 是否为空
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        // 返回部署 URL
        return ResultUtils.success(deployUrl);
    }

    /**
     * 部署指定版本
     */
    @PostMapping("/deploy-version")
    public BaseResponse<String> deployVersion(@RequestBody AppVersionDeployRequest deployRequest,
                                              HttpServletRequest request) {
        ThrowUtils.throwIf(deployRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(deployRequest.getAppId() == null || deployRequest.getAppId() <= 0,
                ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(deployRequest.getVersionId() == null || deployRequest.getVersionId() <= 0,
                ErrorCode.PARAMS_ERROR, "版本 ID 不能为空");
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(appService.deployVersion(
                deployRequest.getAppId(), deployRequest.getVersionId(), loginUser));
    }

    /**
     * 回滚到指定版本
     */
    @PostMapping("/rollback")
    public BaseResponse<String> rollbackVersion(@RequestBody AppVersionDeployRequest rollbackRequest,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(rollbackRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(rollbackRequest.getAppId() == null || rollbackRequest.getAppId() <= 0,
                ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(rollbackRequest.getVersionId() == null || rollbackRequest.getVersionId() <= 0,
                ErrorCode.PARAMS_ERROR, "版本 ID 不能为空");
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(appService.rollbackVersion(
                rollbackRequest.getAppId(), rollbackRequest.getVersionId(), loginUser));
    }

    /**
     * 查询应用版本列表
     */
    @GetMapping("/{appId}/versions")
    public BaseResponse<List<ProjectVersionVO>> listAppVersions(@PathVariable Long appId,
                                                                HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(appService.listAppVersions(appId, loginUser));
    }

    /**
     * 查询应用部署记录
     */
    @GetMapping("/{appId}/deploy-records")
    public BaseResponse<List<DeployRecordVO>> listDeployRecords(@PathVariable Long appId,
                                                                HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(appService.listDeployRecords(appId, loginUser));
    }

    /**
     * 下载应用代码
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        // 1. 基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 2. 查询应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验：只有应用创建者可以下载代码
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码");
        }
        // 4. 构建应用代码目录路径（生成目录，非部署目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 5. 检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");
        // 6. 生成下载文件名（不建议添加中文内容）
        String downloadFileName = String.valueOf(appId);
        // 7. 调用通用下载服务
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }

}
