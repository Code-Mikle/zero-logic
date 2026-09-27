package com.mikle.zerologic.generation.workflow.node;

import com.mikle.zerologic.generation.repair.config.RepairProperties;
import com.mikle.zerologic.generation.repair.model.result.CodeRepairResult;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.generation.repair.service.CodeRepairService;
import com.mikle.zerologic.generation.task.service.GenerationTaskProgressService;
import com.mikle.zerologic.generation.workflow.model.GenerationWorkflowContext;
import jakarta.annotation.Resource;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Component
public class AutoRepairNode {
    @Resource private CodeRepairService codeRepairService;
    @Resource private GenerationTaskProgressService taskProgressService;
    @Resource private RepairProperties repairProperties;

    public AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            GenerationWorkflowContext context = GenerationWorkflowContext.getContext(state);
            if (context == null || context.getBuildDiagnosis() == null
                    || !context.getBuildDiagnosis().isRepairable()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Build failure is not repairable");
            }
            int attempt = context.getRepairAttempt() == null ? 1 : context.getRepairAttempt() + 1;
            if (attempt > repairProperties.getMaxAttempts()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Automatic repair limit reached");
            }
            context.setCurrentStep("auto_repair");
            taskProgressService.updateStep(context.getTaskId(), "auto_repair_" + attempt);
            CodeRepairResult result = codeRepairService.repair(context.getTaskId(), context.getAppId(),
                    context.getUserId(), attempt, Path.of(context.getGeneratedProjectDir()),
                    context.getBuildResult(), context.getBuildDiagnosis(), context.getRepairMemory());
            context.setRepairAttempt(attempt);
            context.setRepairResult(result);
            if (result.isSuccess()) {
                context.setBuildAttempt(context.getBuildAttempt() + 1);
                context.setBuildDiagnosis(null);
            }
            return GenerationWorkflowContext.saveContext(context);
        });
    }
}
