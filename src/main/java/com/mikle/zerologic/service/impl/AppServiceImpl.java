package com.mikle.zerologic.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mikle.zerologic.workflow.generation.GenerationWorkflowRequest;
import com.mikle.zerologic.workflow.generation.GenerationWorkflowService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mikle.zerologic.ai.AiCodeGenTypeRoutingService;
import com.mikle.zerologic.ai.AiCodeGenTypeRoutingServiceFactory;
import com.mikle.zerologic.constant.AppConstant;
import com.mikle.zerologic.core.handler.StreamHandlerExecutor;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.exception.ThrowUtils;
import com.mikle.zerologic.mapper.AppMapper;
import com.mikle.zerologic.model.dto.app.AppAddRequest;
import com.mikle.zerologic.model.dto.app.AppQueryRequest;
import com.mikle.zerologic.model.entity.App;
import com.mikle.zerologic.model.entity.DeployRecord;
import com.mikle.zerologic.model.entity.ProjectVersion;
import com.mikle.zerologic.model.entity.User;
import com.mikle.zerologic.model.enums.ChatHistoryMessageTypeEnum;
import com.mikle.zerologic.model.enums.CodeGenTypeEnum;
import com.mikle.zerologic.model.enums.DeployTypeEnum;
import com.mikle.zerologic.model.vo.AppVO;
import com.mikle.zerologic.model.vo.DeployRecordVO;
import com.mikle.zerologic.model.vo.PromptAttachmentVO;
import com.mikle.zerologic.model.vo.ProjectVersionVO;
import com.mikle.zerologic.model.vo.UserVO;
import com.mikle.zerologic.monitor.MonitorContext;
import com.mikle.zerologic.monitor.MonitorContextHolder;
import com.mikle.zerologic.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Value("${code.deploy-host:http://localhost}")
    private String deployHost;

    @Resource
    private UserService userService;

    @Resource
    private PromptAttachmentService promptAttachmentService;

    @Resource
    private GenerationWorkflowService generationWorkflowService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private ProjectVersionService projectVersionService;

    @Resource
    private DeployRecordService deployRecordService;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    @Resource
    private GenerationAppLockService generationAppLockService;

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, String displayMessage, User loginUser, Long attachmentId) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验，仅本人可以和自己的应用对话
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码生成类型错误");
        }

        String permitId = generationAppLockService.acquire(appId);

        try {
            // 5. 在调用 AI 前，先保存用户消息到数据库中
            chatHistoryService.addChatMessage(appId, displayMessage,
                    ChatHistoryMessageTypeEnum.USER.getValue(),
                    loginUser.getId(),
                    attachmentId,
                    null
            );
            // 6. 设置监控上下文（用户 ID 和应用 ID）
            MonitorContextHolder.setContext(
                    MonitorContext.builder()
                            .userId(loginUser.getId().toString())
                            .appId(appId.toString())
                            .build()
            );
            // 7. 调用 AI 生成代码（流式）
            Flux<String> codeStream = generationWorkflowService.streamGenerate(
                    new GenerationWorkflowRequest(
                            null,
                            appId,
                            loginUser.getId(),
                            message,
                            displayMessage,
                            codeGenTypeEnum,
                            attachmentId
                    )
            );
            // 8. 收集 AI 响应的内容，并且在完成后保存记录到对话历史
            return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser,
                            codeGenTypeEnum, attachmentId, null)
                    .doFinally(
                            signalType -> {
                                generationAppLockService.release(appId, permitId);
                                MonitorContextHolder.clearContext();
                            }
                    );
        } catch (RuntimeException | Error e) {
            generationAppLockService.release(appId, permitId);
            MonitorContextHolder.clearContext();
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR,
                "初始化 prompt 不能为空");

        Long attachmentId = appAddRequest.getAttachmentId();
        // 统一验证：存在、属于当前用户、temporary、尚未绑定
        if (attachmentId != null) {
            promptAttachmentService.getTemporaryAttachment(
                    attachmentId,
                    loginUser.getId()
            );
        }
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 10 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 10)));
        // 使用 AI 智能选择代码生成类型（多例模式）
        AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        app.setInitAttachmentId(attachmentId);
        // 插入数据库
        boolean saved = this.save(app);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "应用创建失败");

        if (attachmentId != null) {
            promptAttachmentService.bindToApp(
                    attachmentId,
                    app.getId(),
                    loginUser.getId()
            );
        }
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        App app = getOwnedApp(appId, loginUser, "无权限部署该应用");
        ProjectVersion latestVersion = projectVersionService.getLatestDeployableVersion(appId, loginUser.getId());
        ThrowUtils.throwIf(latestVersion == null, ErrorCode.NOT_FOUND_ERROR,
                "未找到可部署版本，请先完成一次生成");
        return deployVersionInternal(app, latestVersion, loginUser, DeployTypeEnum.DEPLOY.getValue());
    }

    @Override
    public String deployVersion(Long appId, Long versionId, User loginUser) {
        App app = getOwnedApp(appId, loginUser, "无权限部署该应用");
        ProjectVersion version = projectVersionService.getDeployableVersion(appId, loginUser.getId(), versionId);
        ThrowUtils.throwIf(version == null, ErrorCode.NOT_FOUND_ERROR,
                "版本不存在、无权访问或不可部署");
        return deployVersionInternal(app, version, loginUser, DeployTypeEnum.DEPLOY.getValue());
    }

    @Override
    public String rollbackVersion(Long appId, Long versionId, User loginUser) {
        App app = getOwnedApp(appId, loginUser, "无权限回滚该应用");
        ProjectVersion version = projectVersionService.getDeployableVersion(appId, loginUser.getId(), versionId);
        ThrowUtils.throwIf(version == null, ErrorCode.NOT_FOUND_ERROR,
                "版本不存在、无权访问或不可回滚");
        return deployVersionInternal(app, version, loginUser, DeployTypeEnum.ROLLBACK.getValue());
    }

    @Override
    public List<ProjectVersionVO> listAppVersions(Long appId, User loginUser) {
        getOwnedApp(appId, loginUser, "无权限查看该应用版本");
        return projectVersionService.listByAppId(appId, loginUser.getId());
    }

    @Override
    public List<DeployRecordVO> listDeployRecords(Long appId, User loginUser) {
        getOwnedApp(appId, loginUser, "无权限查看该应用部署记录");
        return deployRecordService.listByAppId(appId, loginUser.getId());
    }

    private String deployVersionInternal(App app, ProjectVersion version, User loginUser, String deployType) {
        Long appId = app.getId();
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 如果没有，则生成 6 位 deployKey（字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 3. 部署固定版本产物，而不是重新构建当前工作目录。
        File sourceDir = new File(version.getArtifactPath());
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "版本产物不存在，请重新生成应用");
        }
        // 4. 复制版本产物到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        DeployRecord deployRecord = deployRecordService.createRunning(
                appId,
                loginUser.getId(),
                version.getId(),
                deployKey,
                deployDirPath,
                deployType
        );
        String appDeployUrl = String.format("%s/%s/", deployHost, deployKey);
        try {
            File deployDir = new File(deployDirPath);
            FileUtil.del(deployDir);
            FileUtil.mkdir(deployDir);
            FileUtil.copyContent(sourceDir, deployDir, true);
            // 7. 更新数据库
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setDeployKey(deployKey);
            updateApp.setDeployedTime(LocalDateTime.now());
            boolean updateResult = this.updateById(updateApp);
            ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
            projectVersionService.markDeployed(version.getId());
            deployRecordService.finishSuccess(deployRecord.getId(), appDeployUrl);
        } catch (Exception e) {
            deployRecordService.finishFailed(deployRecord.getId(), e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署失败：" + e.getMessage());
        }
        // 8. 构建应用访问 URL
        // 9. 异步生成截图并且更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    private App getOwnedApp(Long appId, User loginUser, String noAuthMessage) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!Objects.equals(app.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, noAuthMessage);
        }
        return app;
    }

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程并执行
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            // 更新数据库的封面
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        PromptAttachmentVO attachmentVO =
                promptAttachmentService.getAttachmentVOById(app.getInitAttachmentId());
        appVO.setPromptAttachmentVO(attachmentVO);
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 删除应用时，关联删除对话历史
     *
     * @param id
     * @return
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        long appId = Long.parseLong(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            log.error("删除应用关联的对话历史失败：{}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }
}
