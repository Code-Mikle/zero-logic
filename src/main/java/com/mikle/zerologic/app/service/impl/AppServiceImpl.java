package com.mikle.zerologic.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mikle.zerologic.app.deployment.service.DeployRecordService;
import com.mikle.zerologic.app.preview.service.ScreenshotService;
import com.mikle.zerologic.app.service.AppService;
import com.mikle.zerologic.app.version.service.ProjectVersionService;
import com.mikle.zerologic.conversation.service.ChatHistoryService;
import com.mikle.zerologic.generation.build.service.GenerationBuildRecordService;
import com.mikle.zerologic.generation.repair.service.GenerationRepairRecordService;
import com.mikle.zerologic.generation.task.service.GenerationAppLockService;
import com.mikle.zerologic.generation.tool.service.ToolCallRecordService;
import com.mikle.zerologic.knowledge.attachment.service.PromptAttachmentService;
import com.mikle.zerologic.knowledge.document.service.KnowledgeChunkService;
import com.mikle.zerologic.knowledge.document.service.KnowledgeDocumentService;
import com.mikle.zerologic.knowledge.embedding.service.KnowledgeEmbeddingService;
import com.mikle.zerologic.knowledge.retrieval.service.RagRetrievalLogService;
import com.mikle.zerologic.user.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mikle.zerologic.generation.codegen.service.AiCodeGenTypeRoutingService;
import com.mikle.zerologic.generation.codegen.service.AiCodeGenTypeRoutingServiceFactory;
import com.mikle.zerologic.app.constant.AppConstant;
import com.mikle.zerologic.app.version.archive.ProjectVersionArchiver;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.exception.ThrowUtils;
import com.mikle.zerologic.app.mapper.AppMapper;
import com.mikle.zerologic.generation.task.mapper.GenerationTaskMapper;
import com.mikle.zerologic.app.model.dto.AdminAppQueryRequest;
import com.mikle.zerologic.app.model.dto.AppAddRequest;
import com.mikle.zerologic.app.model.dto.GoodAppPageQueryRequest;
import com.mikle.zerologic.app.model.dto.MyAppQueryRequest;
import com.mikle.zerologic.app.model.entity.App;
import com.mikle.zerologic.app.deployment.model.entity.DeployRecord;
import com.mikle.zerologic.app.version.model.entity.ProjectVersion;
import com.mikle.zerologic.user.model.entity.User;
import com.mikle.zerologic.generation.codegen.model.enums.CodeGenTypeEnum;
import com.mikle.zerologic.app.deployment.model.enums.DeployTypeEnum;
import com.mikle.zerologic.app.model.vo.AppVO;
import com.mikle.zerologic.app.deployment.model.vo.DeployRecordVO;
import com.mikle.zerologic.app.model.vo.GoodAppVO;
import com.mikle.zerologic.knowledge.attachment.model.vo.PromptAttachmentVO;
import com.mikle.zerologic.app.version.model.vo.ProjectVersionVO;
import com.mikle.zerologic.user.model.vo.UserVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final int MAX_MY_APP_PAGE_SIZE = 20;

    private static final int MAX_ADMIN_APP_PAGE_SIZE = 100;

    private static final int MAX_GOOD_APP_PAGE_SIZE = 20;

    private static final Set<String> APP_SORT_FIELDS = Set.of(
            "id", "appName", "priority", "deployedTime", "createTime", "updateTime"
    );

    @Value("${code.deploy-host:http://localhost}")
    private String deployHost;

    @Resource
    private UserService userService;

    @Resource
    private CacheManager cacheManager;

    @Resource
    private PromptAttachmentService promptAttachmentService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ProjectVersionService projectVersionService;

    @Resource
    private DeployRecordService deployRecordService;

    @Resource
    private ProjectVersionArchiver projectVersionArchiver;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    @Resource
    private GenerationAppLockService generationAppLockService;

    @Resource
    private GenerationTaskMapper generationTaskMapper;

    @Resource
    private GenerationBuildRecordService generationBuildRecordService;

    @Resource
    private GenerationRepairRecordService generationRepairRecordService;

    @Resource
    private ToolCallRecordService toolCallRecordService;

    @Resource
    private RagRetrievalLogService ragRetrievalLogService;

    @Resource
    private KnowledgeEmbeddingService knowledgeEmbeddingService;

    @Resource
    private KnowledgeChunkService knowledgeChunkService;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

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
            promptAttachmentService.validateTemporaryAttachment(
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

    /**
     * 删除应用时，关联删除对话历史
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        long appId = Long.parseLong(id.toString());
        if (appId <= 0) {
            return false;
        }
        App app = this.getById(appId);
        if (app == null) {
            return false;
        }
        deleteRelatedRecords(appId);
        boolean removed = super.removeById(id);
        if (removed) {
            deleteGeneratedFiles(app);
        }
        return removed;
    }

    @Override
    @CacheEvict(value = AppConstant.GOOD_APP_CACHE_NAME, allEntries = true)
    public String deployApp(Long appId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String permitId = generationAppLockService.acquire(appId);
        try {
            App app = getOwnedApp(appId, loginUser, "无权限部署该应用");
            ProjectVersion latestVersion = projectVersionService.getLatestDeployableVersion(appId, loginUser.getId());
            ThrowUtils.throwIf(latestVersion == null, ErrorCode.NOT_FOUND_ERROR,
                    "未找到可部署版本，请先完成一次生成");
            return deployVersionInternal(app, latestVersion, loginUser, DeployTypeEnum.DEPLOY.getValue());
        } finally {
            generationAppLockService.release(appId, permitId);
        }
    }

    @Override
    @CacheEvict(value = AppConstant.GOOD_APP_CACHE_NAME, allEntries = true)
    public String deployVersion(Long appId, Long versionId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(versionId == null || versionId <= 0, ErrorCode.PARAMS_ERROR, "版本 ID 错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String permitId = generationAppLockService.acquire(appId);
        try {
            App app = getOwnedApp(appId, loginUser, "无权限部署该应用");
            ProjectVersion version = projectVersionService.getDeployableVersion(appId, loginUser.getId(), versionId);
            ThrowUtils.throwIf(version == null, ErrorCode.NOT_FOUND_ERROR,
                    "版本不存在、无权访问或不可部署");
            return deployVersionInternal(app, version, loginUser, DeployTypeEnum.DEPLOY.getValue());
        } finally {
            generationAppLockService.release(appId, permitId);
        }
    }

    @Override
    @CacheEvict(value = AppConstant.GOOD_APP_CACHE_NAME, allEntries = true)
    public String rollbackVersion(Long appId, Long versionId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(versionId == null || versionId <= 0, ErrorCode.PARAMS_ERROR, "版本 ID 错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String permitId = generationAppLockService.acquire(appId);
        try {
            App app = getOwnedApp(appId, loginUser, "无权限回滚该应用");
            ProjectVersion version = projectVersionService.getDeployableVersion(appId, loginUser.getId(), versionId);
            ThrowUtils.throwIf(version == null, ErrorCode.NOT_FOUND_ERROR,
                    "版本不存在、无权访问或不可回滚");
            String appDeployUrl = deployVersionInternal(app, version, loginUser, DeployTypeEnum.ROLLBACK.getValue());
            truncateVersionsAfterRollback(appId, loginUser.getId(), version.getVersionNo());
            return appDeployUrl;
        } finally {
            generationAppLockService.release(appId, permitId);
        }
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
        String appDeployUrl = String.format("%s/%s/?v=%s&t=%s",
                deployHost, deployKey, version.getVersionNo(), System.currentTimeMillis());
        try {
            File deployDir = new File(deployDirPath);
            FileUtil.del(deployDir);
            FileUtil.mkdir(deployDir);
            FileUtil.copyContent(sourceDir, deployDir, true);
            syncPreviewDirectory(appId, version, sourceDir);
            // 7. 更新数据库
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setDeployKey(deployKey);
            updateApp.setDeployedTime(LocalDateTime.now());
            boolean updateResult = this.updateById(updateApp);
            ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
            projectVersionService.markCurrentDeployed(appId, loginUser.getId(), version.getId());
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

    private void truncateVersionsAfterRollback(Long appId, Long userId, Integer rollbackVersionNo) {
        List<ProjectVersion> obsoleteVersions = projectVersionService.listAfterVersionNo(appId, userId, rollbackVersionNo);
        if (obsoleteVersions.isEmpty()) {
            return;
        }
        List<Long> obsoleteVersionIds = obsoleteVersions.stream()
                .map(ProjectVersion::getId)
                .filter(Objects::nonNull)
                .toList();
        deployRecordService.physicalDeleteByVersionIds(appId, userId, obsoleteVersionIds);
        int deletedVersionCount = projectVersionService.physicalDeleteAfterVersionNo(appId, userId, rollbackVersionNo);
        projectVersionArchiver.deleteArchivedVersions(obsoleteVersions);
        log.info("应用版本回滚后截断完成: appId={}, rollbackVersionNo={}, deletedVersionCount={}",
                appId, rollbackVersionNo, deletedVersionCount);
    }

    private void syncPreviewDirectory(Long appId, ProjectVersion version, File sourceDir) {
        String previewDirName = version.getCodeGenType() + "_" + appId;
        File previewDir = new File(AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + previewDirName);
        FileUtil.del(previewDir);
        FileUtil.mkdir(previewDir);
        if (CodeGenTypeEnum.VUE_PROJECT.getValue().equals(version.getCodeGenType())) {
            File distDir = new File(previewDir, "dist");
            FileUtil.mkdir(distDir);
            FileUtil.copyContent(sourceDir, distDir, true);
            return;
        }
        FileUtil.copyContent(sourceDir, previewDir, true);
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
            clearGoodAppCache();
        });
    }

    private void clearGoodAppCache() {
        Cache cache = cacheManager.getCache(AppConstant.GOOD_APP_CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
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
        // 批量获取关联数据，避免逐条查询产生 N+1 问题。
        Map<Long, UserVO> userVOMap = getUserVOMap(appList);

        Set<Long> attachmentIds = appList.stream()
                .map(App::getInitAttachmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, PromptAttachmentVO> attachmentVOMap =
                promptAttachmentService.getAttachmentVOMapByIds(attachmentIds);

        return appList.stream().map(app -> {
            AppVO appVO = new AppVO();
            BeanUtil.copyProperties(app, appVO);
            appVO.setUser(userVOMap.get(app.getUserId()));
            appVO.setPromptAttachmentVO(attachmentVOMap.get(app.getInitAttachmentId()));
            return appVO;
        }).collect(Collectors.toList());
    }

    private Map<Long, UserVO> getUserVOMap(List<App> appList) {
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
    }

    @Override
    public Page<AppVO> pageMyApps(MyAppQueryRequest queryRequest, Long userId) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR, "查询参数为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.NOT_LOGIN_ERROR);

        int pageNum = queryRequest.getPageNum();
        int pageSize = queryRequest.getPageSize();
        ThrowUtils.throwIf(pageNum <= 0, ErrorCode.PARAMS_ERROR, "页码必须大于 0");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > MAX_MY_APP_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "每页查询数量必须在 1 到 20 之间");

        String sortField = StrUtil.blankToDefault(queryRequest.getSortField(), "createTime");
        ThrowUtils.throwIf(!APP_SORT_FIELDS.contains(sortField),
                ErrorCode.PARAMS_ERROR, "不支持的排序字段");
        boolean ascending = parseSortDirection(queryRequest.getSortOrder());

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", userId)
                .like("appName", queryRequest.getAppName(), StrUtil.isNotBlank(queryRequest.getAppName()))
                .eq("codeGenType", queryRequest.getCodeGenType(),
                        StrUtil.isNotBlank(queryRequest.getCodeGenType()))
                .orderBy(sortField, ascending);
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), queryWrapper);

        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        appVOPage.setRecords(getAppVOList(appPage.getRecords()));
        return appVOPage;
    }

    @Override
    public Page<AppVO> pageAdminApps(AdminAppQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR, "查询参数为空");

        int pageNum = queryRequest.getPageNum();
        int pageSize = queryRequest.getPageSize();
        ThrowUtils.throwIf(pageNum <= 0, ErrorCode.PARAMS_ERROR, "页码必须大于 0");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > MAX_ADMIN_APP_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "每页查询数量必须在 1 到 100 之间");

        String sortField = StrUtil.blankToDefault(queryRequest.getSortField(), "createTime");
        ThrowUtils.throwIf(!APP_SORT_FIELDS.contains(sortField),
                ErrorCode.PARAMS_ERROR, "不支持的排序字段");
        boolean ascending = parseSortDirection(queryRequest.getSortOrder());

        QueryWrapper queryWrapper = buildAdminQueryWrapper(queryRequest, sortField, ascending);
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), queryWrapper);

        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        appVOPage.setRecords(getAppVOList(appPage.getRecords()));
        return appVOPage;
    }

    @Override
    @Cacheable(
            value = AppConstant.GOOD_APP_CACHE_NAME,
            key = "#queryRequest.pageNum + ':' + #queryRequest.pageSize",
            condition = "#queryRequest != null && #queryRequest.pageNum <= 10",
            sync = true
    )
    public Page<GoodAppVO> pageGoodApps(GoodAppPageQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR, "查询参数为空");

        int pageNum = queryRequest.getPageNum();
        int pageSize = queryRequest.getPageSize();
        ThrowUtils.throwIf(pageNum <= 0, ErrorCode.PARAMS_ERROR, "页码必须大于 0");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > MAX_GOOD_APP_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "每页查询数量必须在 1 到 20 之间");

        Page<App> appPage = this.page(
                Page.of(pageNum, pageSize),
                buildGoodAppQueryWrapper()
        );
        Page<GoodAppVO> goodAppPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        goodAppPage.setRecords(getGoodAppVOList(appPage.getRecords()));
        return goodAppPage;
    }

    QueryWrapper buildGoodAppQueryWrapper() {
        return QueryWrapper.create()
                .select("id", "appName", "cover", "codeGenType", "deployKey", "userId")
                .eq("priority", AppConstant.GOOD_APP_PRIORITY)
                .orderBy("createTime", false)
                .orderBy("id", false);
    }

    List<GoodAppVO> getGoodAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        Map<Long, UserVO> userVOMap = getUserVOMap(appList);
        return appList.stream().map(app -> {
            GoodAppVO goodAppVO = new GoodAppVO();
            BeanUtil.copyProperties(app, goodAppVO);
            goodAppVO.setUser(userVOMap.get(app.getUserId()));
            return goodAppVO;
        }).collect(Collectors.toList());
    }

    QueryWrapper buildAdminQueryWrapper(AdminAppQueryRequest queryRequest,
                                        String sortField,
                                        boolean ascending) {
        return QueryWrapper.create()
                .eq("id", queryRequest.getId(), queryRequest.getId() != null)
                .like("appName", queryRequest.getAppName(), StrUtil.isNotBlank(queryRequest.getAppName()))
                .eq("codeGenType", queryRequest.getCodeGenType(),
                        StrUtil.isNotBlank(queryRequest.getCodeGenType()))
                .eq("deployKey", queryRequest.getDeployKey(), StrUtil.isNotBlank(queryRequest.getDeployKey()))
                .eq("priority", queryRequest.getPriority(), queryRequest.getPriority() != null)
                .eq("userId", queryRequest.getUserId(), queryRequest.getUserId() != null)
                .orderBy(sortField, ascending);
    }

    private boolean parseSortDirection(String sortOrder) {
        if (StrUtil.isBlank(sortOrder)
                || "desc".equalsIgnoreCase(sortOrder)
                || "descend".equalsIgnoreCase(sortOrder)) {
            return false;
        }
        if ("asc".equalsIgnoreCase(sortOrder)
                || "ascend".equalsIgnoreCase(sortOrder)) {
            return true;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的排序方式");
    }

    private void deleteRelatedRecords(Long appId) {
        safeRemove("chat_history", () -> chatHistoryService.deleteByAppId(appId));
        safeRemove("prompt_attachment", () -> promptAttachmentService.remove(QueryWrapper.create().eq("appId", appId)));
        safeRemove("rag_retrieval_log", () -> ragRetrievalLogService.remove(QueryWrapper.create().eq("appId", appId)));
        safeRemove("knowledge_embedding", () -> knowledgeEmbeddingService.remove(QueryWrapper.create().eq("appId", appId)));
        safeRemove("knowledge_chunk", () -> knowledgeChunkService.remove(QueryWrapper.create().eq("appId", appId)));
        safeRemove("knowledge_document", () -> knowledgeDocumentService.remove(QueryWrapper.create().eq("appId", appId)));
        safeRemove("tool_call_record", () -> toolCallRecordService.remove(QueryWrapper.create().eq("appId", appId)));
        safeRemove("generation_repair_record", () -> generationRepairRecordService.remove(QueryWrapper.create().eq("appId", appId)));
        safeRemove("generation_build_record", () -> generationBuildRecordService.remove(QueryWrapper.create().eq("appId", appId)));
        safeRemove("generation_task", () -> generationTaskMapper.deleteByQuery(QueryWrapper.create().eq("appId", appId)));
        safeRemove("deploy_record", () -> deployRecordService.remove(QueryWrapper.create().eq("appId", appId)));
        safeRemove("project_version", () -> projectVersionService.remove(QueryWrapper.create().eq("appId", appId)));
    }

    private void safeRemove(String tableName, Runnable removeAction) {
        try {
            removeAction.run();
        } catch (Exception e) {
            log.warn("删除应用关联数据失败: table={}, reason={}", tableName, e.getMessage(), e);
        }
    }

    private void deleteGeneratedFiles(App app) {
        Long appId = app.getId();
        safeDeleteDirectory("preview", new File(AppConstant.CODE_OUTPUT_ROOT_DIR
                + File.separator + app.getCodeGenType() + "_" + appId));
        if (StrUtil.isNotBlank(app.getDeployKey())) {
            safeDeleteDirectory("deploy", new File(AppConstant.CODE_DEPLOY_ROOT_DIR
                    + File.separator + app.getDeployKey()));
        }
        safeDeleteDirectory("project_versions", new File(AppConstant.PROJECT_VERSION_ROOT_DIR
                + File.separator + "app_" + appId));
    }

    private void safeDeleteDirectory(String name, File directory) {
        try {
            FileUtil.del(directory);
        } catch (Exception e) {
            log.warn("删除应用本地产物失败: name={}, path={}, reason={}",
                    name, directory.getAbsolutePath(), e.getMessage(), e);
        }
    }
}
