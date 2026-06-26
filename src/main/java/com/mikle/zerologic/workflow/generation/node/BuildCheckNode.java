package com.mikle.zerologic.workflow.generation.node;

import com.mikle.zerologic.core.build.GeneratedProjectPathResolver;
import com.mikle.zerologic.core.build.model.BuildResult;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.service.GenerationTaskProgressService;
import com.mikle.zerologic.service.ProjectBuildService;
import com.mikle.zerologic.workflow.generation.GenerationWorkflowContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Component
@Slf4j
public class BuildCheckNode {

    @Resource
    private GeneratedProjectPathResolver projectPathResolver;

    @Resource
    private ProjectBuildService projectBuildService;

    @Resource
    private GenerationTaskProgressService taskProgressService;

    public AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            GenerationWorkflowContext context = GenerationWorkflowContext.getContext(state);
            if (context == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成工作流上下文不存在");
            }
            context.setCurrentStep("build_check");
            taskProgressService.updateStep(context.getTaskId(), "build_check");

            Path projectPath = projectPathResolver.resolve(context.getAppId(), context.getCodeGenType());
            BuildResult buildResult = projectBuildService.build(
                    context.getTaskId(), context.getAppId(), context.getUserId(),
                    context.getCodeGenType(), projectPath, context.getBuildAttempt());
            context.setGeneratedProjectDir(projectPath.toString());
            context.setBuildResult(buildResult);
            log.info("构建检查完成，taskId={}, success={}, recordId={}",
                    context.getTaskId(), buildResult.getSuccess(), buildResult.getBuildRecordId());
            return GenerationWorkflowContext.saveContext(context);
        });
    }
}
