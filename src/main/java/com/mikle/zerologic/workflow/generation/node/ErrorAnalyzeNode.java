package com.mikle.zerologic.workflow.generation.node;

import com.mikle.zerologic.core.build.BuildLogAnalyzer;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.service.GenerationTaskProgressService;
import com.mikle.zerologic.workflow.generation.GenerationWorkflowContext;
import jakarta.annotation.Resource;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Component
public class ErrorAnalyzeNode {
    @Resource private BuildLogAnalyzer buildLogAnalyzer;
    @Resource private GenerationTaskProgressService taskProgressService;

    public AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            GenerationWorkflowContext context = GenerationWorkflowContext.getContext(state);
            if (context == null || context.getBuildResult() == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Missing failed build context");
            }
            context.setCurrentStep("error_analyze");
            taskProgressService.updateStep(context.getTaskId(), "error_analyze");
            context.setBuildDiagnosis(buildLogAnalyzer.analyze(context.getCodeGenType(), context.getBuildResult()));
            return GenerationWorkflowContext.saveContext(context);
        });
    }
}
