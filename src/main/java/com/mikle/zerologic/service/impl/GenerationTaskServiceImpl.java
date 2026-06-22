package com.mikle.zerologic.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mikle.zerologic.core.handler.StreamHandlerExecutor;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.exception.ThrowUtils;
import com.mikle.zerologic.mapper.GenerationTaskMapper;
import com.mikle.zerologic.model.dto.generationtask.GenerationTaskCreateRequest;
import com.mikle.zerologic.model.entity.App;
import com.mikle.zerologic.model.entity.GenerationTask;
import com.mikle.zerologic.model.entity.PromptAttachment;
import com.mikle.zerologic.model.entity.User;
import com.mikle.zerologic.model.enums.ChatHistoryMessageTypeEnum;
import com.mikle.zerologic.model.enums.CodeGenTypeEnum;
import com.mikle.zerologic.model.enums.GenerationTaskStatusEnum;
import com.mikle.zerologic.model.enums.GenerationTaskTypeEnum;
import com.mikle.zerologic.model.vo.GenerationTaskVO;
import com.mikle.zerologic.service.*;
import com.mikle.zerologic.workflow.generation.GenerationWorkflowRequest;
import com.mikle.zerologic.workflow.generation.GenerationWorkflowService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.time.LocalDateTime;
import java.util.Objects;

import static com.mikle.zerologic.constant.PromptLimitConstant.MAX_ATTACHMENT_CONTENT_LENGTH;
import static com.mikle.zerologic.constant.PromptLimitConstant.MAX_MODEL_MESSAGE_LENGTH;
import static com.mikle.zerologic.constant.PromptLimitConstant.MAX_USER_PROMPT_LENGTH;

@Service
@Slf4j
public class GenerationTaskServiceImpl extends ServiceImpl<GenerationTaskMapper, GenerationTask> implements GenerationTaskService {

    @Resource
    private AppService appService;

    @Resource
    private PromptAttachmentService promptAttachmentService;

    @Resource
    private GenerationAppLockService generationAppLockService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private GenerationWorkflowService generationWorkflowService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private KnowledgeIngestService knowledgeIngestService;

    @Override
    public Long createGenerateTask(GenerationTaskCreateRequest request, User loginUser) {

        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(request.getAppId() == null || request.getAppId() <= 0,
                ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getMessage()),
                ErrorCode.PARAMS_ERROR, "提示词不能为空");
        ThrowUtils.throwIf(request.getMessage().length() > MAX_USER_PROMPT_LENGTH,
                ErrorCode.PARAMS_ERROR, "提示词不能超过 1000 字");

        QueryWrapper appQuery = QueryWrapper.create()
                .eq("id", request.getAppId())
                .eq("userId", loginUser.getId());

        App app = appService.getOne(appQuery);
        ThrowUtils.throwIf(app == null, ErrorCode.NO_AUTH_ERROR, "应用不存在或无权访问");

        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码生成类型错误");
        }

        String modelPrompt = request.getMessage();
        if (request.getAttachmentId() != null) {
            PromptAttachment usableAttachment = promptAttachmentService.getUsableAttachment(
                            request.getAttachmentId(),
                            loginUser.getId(),
                            app.getId()
            );

            String attachmentContent = usableAttachment.getContent();
            ThrowUtils.throwIf(attachmentContent == null || attachmentContent.length() > MAX_ATTACHMENT_CONTENT_LENGTH,
                    ErrorCode.PARAMS_ERROR, "附件提取文本不能超过 20000 字");

            knowledgeIngestService.ingestAttachment(
                    request.getAttachmentId(),
                    app.getId(),
                    loginUser
            );
        }
        ThrowUtils.throwIf(modelPrompt.length() > MAX_MODEL_MESSAGE_LENGTH, ErrorCode.PARAMS_ERROR,
                "输入模型的文本不能超过 22000 字");

        GenerationTask generationTask = GenerationTask.builder()
                .appId(app.getId())
                .userId(loginUser.getId())
                .attachmentId(request.getAttachmentId())
                .taskType(GenerationTaskTypeEnum.GENERATE.getValue())
                .status(GenerationTaskStatusEnum.PENDING.getValue())
                .currentStep("pending")
                .inputPrompt(request.getMessage())
                .modelPrompt(modelPrompt)
                .codeGenType(app.getCodeGenType())
                .tokenUsage(0L)
                .toolCallCount(0)
                .build();

        boolean saved = this.save(generationTask);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "任务创建失败");

        return generationTask.getId();
    }

    @Override
    public GenerationTaskVO getTaskVO(Long taskId, User loginUser) {
        GenerationTask task = getOwnedTask(taskId, loginUser);
        return toVO(task);
    }

    @Override
    public Flux<String> streamGenerateTask(Long taskId, User loginUser) {

        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        GenerationTask task = this.getById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR,
                "请求的 task id 不存在");
        ThrowUtils.throwIf(!Objects.equals(task.getUserId(), loginUser.getId()), ErrorCode.NO_AUTH_ERROR,
                "该 task 不属于当前用户");
        ThrowUtils.throwIf(!GenerationTaskStatusEnum.PENDING.getValue().equalsIgnoreCase(task.getStatus()),
                ErrorCode.PARAMS_ERROR,
                "请求的 task 状态不对");

        String permitId = generationAppLockService.acquire(task.getAppId());

        try {
            updateTaskRunning(taskId);

            chatHistoryService.addChatMessage(
                    task.getAppId(),
                    task.getInputPrompt(),
                    ChatHistoryMessageTypeEnum.USER.getValue(),
                    loginUser.getId(),
                    task.getAttachmentId()
            );

            String codeGenType = task.getCodeGenType();
            CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
            if (codeGenTypeEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码生成类型错误");
            }
            Flux<String> codeStream = generationWorkflowService.streamGenerate(
                    new GenerationWorkflowRequest(
                            task.getId(),
                            task.getAppId(),
                            task.getUserId(),
                            task.getModelPrompt(),
                            task.getInputPrompt(),
                            codeGenTypeEnum,
                            task.getAttachmentId()
                    )
            );

            Long appId = task.getAppId();
            Long attachmentId = task.getAttachmentId();
            return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, codeGenTypeEnum, attachmentId)
                    .doOnComplete(() -> updateTaskSuccess(taskId))
                    .doOnError(e -> updateTaskFailed(taskId, e))
                    .doFinally(
                            signalType -> {
                                if (signalType == SignalType.CANCEL) {
                                    updateTaskCanceled(taskId, "客户端断开连接，生成任务已取消");
                                }
                                generationAppLockService.release(appId, permitId);
                            }
                    );
        } catch (Exception e) {
            generationAppLockService.release(task.getAppId(), permitId);
            updateTaskFailed(taskId, e);
            throw e;
        }
    }

    private void updateTaskSuccess(Long taskId) {
        GenerationTask task = GenerationTask.builder()
                .id(taskId)
                .status(GenerationTaskStatusEnum.SUCCESS.getValue())
                .endTime(LocalDateTime.now())
                .currentStep("done")
                .build();

        boolean updated = this.updateById(task);
        if (!updated) {
            log.warn("updateTaskSuccess 更新 generation_task 表失败，taskId={}", taskId);
        }
    }

    private void updateTaskFailed(Long taskId, Throwable e) {
        String errorMessage = e.getMessage();
        if (StrUtil.isBlank(errorMessage)) {
            errorMessage = e.getClass().getSimpleName();
        }
        GenerationTask task = GenerationTask.builder()
                .id(taskId)
                .status(GenerationTaskStatusEnum.FAILED.getValue())
                .endTime(LocalDateTime.now())
                .currentStep("failed")
                .errorMessage(StrUtil.subPre(errorMessage, 2000))
                .build();

        boolean updated = this.updateById(task);
        if (!updated) {
            log.warn("updateTaskFailed 更新 generation_task 表失败，taskId={}", taskId);
        }
    }

    private void updateTaskCanceled(Long taskId, String reason) {
        GenerationTask task = GenerationTask.builder()
                .id(taskId)
                .status(GenerationTaskStatusEnum.CANCELED.getValue())
                .endTime(LocalDateTime.now())
                .currentStep("canceled")
                .errorMessage(reason)
                .build();

        boolean updated = this.updateById(task);
        if (!updated) {
            log.warn("updateTaskCanceled 更新 generation_task 表失败，taskId={}", taskId);
        }
    }

    private void updateTaskRunning(Long taskId) {
        GenerationTask updateTask = GenerationTask.builder()
                .id(taskId)
                .status(GenerationTaskStatusEnum.RUNNING.getValue())
                .currentStep("prepare_context")
                .startTime(LocalDateTime.now())
                .build();

        boolean updated = this.updateById(updateTask);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR,
                "updateTaskRunning 更新 generation_task 表失败");
    }

    @Override
    public Boolean cancelTask(Long taskId, User loginUser) {
        GenerationTask task = getOwnedTask(taskId, loginUser);
        if (GenerationTaskStatusEnum.PENDING.getValue().equals(task.getStatus())) {
            GenerationTask updateTask = GenerationTask.builder()
                    .id(taskId)
                    .status(GenerationTaskStatusEnum.CANCELED.getValue())
                    .currentStep("canceled")
                    .endTime(LocalDateTime.now())
                    .build();
            boolean updated = this.updateById(updateTask);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "取消任务失败");
            return true;
        }
        ThrowUtils.throwIf(GenerationTaskStatusEnum.RUNNING.getValue().equals(task.getStatus()),
                ErrorCode.OPERATION_ERROR, "暂不支持取消执行中的任务");
        return false;
    }

    private GenerationTask getOwnedTask(Long taskId, User loginUser) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR, "任务 ID 错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        GenerationTask task = this.getById(taskId);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");
        ThrowUtils.throwIf(!Objects.equals(task.getUserId(), loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权访问该任务");
        return task;
    }

    private GenerationTaskVO toVO(GenerationTask task) {
        GenerationTaskVO vo = new GenerationTaskVO();
        BeanUtil.copyProperties(task, vo);
        return vo;
    }
}
