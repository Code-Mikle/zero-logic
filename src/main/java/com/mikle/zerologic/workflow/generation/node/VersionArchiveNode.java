package com.mikle.zerologic.workflow.generation.node;

import com.mikle.zerologic.core.version.ProjectVersionArchiver;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.model.entity.ProjectVersion;
import com.mikle.zerologic.service.GenerationTaskProgressService;
import com.mikle.zerologic.workflow.generation.GenerationWorkflowContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Component
public class VersionArchiveNode {

    @Resource
    private ProjectVersionArchiver projectVersionArchiver;

    @Resource
    private GenerationTaskProgressService taskProgressService;

    public AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            GenerationWorkflowContext context = GenerationWorkflowContext.getContext(state);
            if (context == null || context.getBuildResult() == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "版本归档上下文不存在");
            }
            context.setCurrentStep("version_archive");
            taskProgressService.updateStep(context.getTaskId(), "version_archive");
            ProjectVersion version = projectVersionArchiver.archiveBuiltVersion(
                    context.getAppId(),
                    context.getUserId(),
                    context.getTaskId(),
                    context.getCodeGenType(),
                    Path.of(context.getGeneratedProjectDir()),
                    context.getBuildResult()
            );
            context.setVersionId(version.getId());
            context.setVersionNo(version.getVersionNo());
            log.info("生成工作流版本归档完成，appId={}, taskId={}, versionId={}, versionNo={}",
                    context.getAppId(), context.getTaskId(), version.getId(), version.getVersionNo());
            return GenerationWorkflowContext.saveContext(context);
        });
    }
}
