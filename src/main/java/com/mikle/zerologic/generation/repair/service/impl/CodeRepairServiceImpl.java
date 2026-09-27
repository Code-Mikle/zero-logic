package com.mikle.zerologic.generation.repair.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mikle.zerologic.generation.codegen.service.AiCodeGeneratorService;
import com.mikle.zerologic.generation.codegen.service.AiCodeGeneratorServiceFactory;
import com.mikle.zerologic.generation.tool.execution.ToolExecutionContext;
import com.mikle.zerologic.generation.tool.execution.ToolExecutionContextHolder;
import com.mikle.zerologic.generation.tool.model.enums.ToolCallSourceEnum;
import com.mikle.zerologic.generation.repair.config.RepairProperties;
import com.mikle.zerologic.generation.build.model.result.BuildDiagnosis;
import com.mikle.zerologic.generation.build.model.result.BuildResult;
import com.mikle.zerologic.generation.repair.snapshot.ProjectSnapshotService;
import com.mikle.zerologic.generation.repair.model.result.CodeRepairResult;
import com.mikle.zerologic.generation.repair.model.entity.GenerationRepairRecord;
import com.mikle.zerologic.generation.codegen.model.enums.CodeGenTypeEnum;
import com.mikle.zerologic.generation.repair.service.CodeRepairService;
import com.mikle.zerologic.generation.repair.service.GenerationRepairRecordService;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.memory.ChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CodeRepairServiceImpl implements CodeRepairService {
    @Resource private AiCodeGeneratorServiceFactory aiServiceFactory;
    @Resource private GenerationRepairRecordService repairRecordService;
    @Resource private ProjectSnapshotService snapshotService;
    @Resource private RepairProperties repairProperties;

    @Override
    public CodeRepairResult repair(Long taskId, Long appId, Long userId, int repairAttempt,
                                   Path projectPath, BuildResult failedBuild, BuildDiagnosis diagnosis,
                                   ChatMemory repairMemory) {
        GenerationRepairRecord record = repairRecordService.createRunning(taskId, appId, userId,
                repairAttempt, failedBuild.getBuildRecordId(), diagnosis);
        long start = System.currentTimeMillis();
        StringBuilder response = new StringBuilder();
        List<String> changedFiles = List.of();
        String status = "failed";
        String errorMessage = null;
        Map<String, byte[]> beforeContents = Map.of();
        Map<String, byte[]> protectedFiles = Map.of();
        try {
            Map<String, String> before = snapshotService.snapshot(projectPath);
            beforeContents = snapshotService.snapshotContents(projectPath);
            protectedFiles = snapshotService.snapshotProtectedFiles(projectPath);
            invokeAgent(taskId, appId, userId, buildPrompt(repairAttempt, diagnosis, failedBuild),
                    response, repairMemory);
            snapshotService.restoreProtectedFiles(projectPath, protectedFiles);
            changedFiles = snapshotService.changedFiles(before, snapshotService.snapshot(projectPath));
            if (changedFiles.isEmpty()) {
                throw new IllegalStateException("Repair agent did not change any project files");
            }
            status = "success";
        } catch (java.util.concurrent.TimeoutException e) {
            status = "timeout";
            errorMessage = "Repair agent timed out";
        } catch (Exception e) {
            errorMessage = StrUtil.blankToDefault(e.getMessage(), e.getClass().getSimpleName());
            log.error("Automatic repair failed, taskId={}, attempt={}", taskId, repairAttempt, e);
        } finally {
            try {
                if (!"success".equals(status) && !beforeContents.isEmpty()) {
                    snapshotService.restoreSnapshot(projectPath, beforeContents);
                    changedFiles = List.of();
                } else if (!protectedFiles.isEmpty()) {
                    snapshotService.restoreProtectedFiles(projectPath, protectedFiles);
                }
            } catch (Exception rollbackError) {
                status = "failed";
                errorMessage = StrUtil.subPre(StrUtil.blankToDefault(errorMessage, "Repair failed")
                        + "; rollback failed: " + rollbackError.getMessage(), 2048);
                log.error("Failed to roll back repair files, taskId={}, attempt={}",
                        taskId, repairAttempt, rollbackError);
            }
        }
        long duration = System.currentTimeMillis() - start;
        repairRecordService.finish(record.getId(), status, changedFiles, response.toString(), errorMessage, duration);
        return CodeRepairResult.builder().success("success".equals(status)).status(status)
                .aiResponse(response.toString()).errorMessage(errorMessage)
                .changedFiles(changedFiles).repairRecordId(record.getId()).build();
    }

    private void invokeAgent(Long taskId, Long appId, Long userId,
                             String prompt, StringBuilder response,
                             ChatMemory repairMemory) throws Exception {
        AiCodeGeneratorService service = aiServiceFactory.createAiCodeGeneratorService(
                CodeGenTypeEnum.VUE_PROJECT, repairMemory);
        CompletableFuture<Void> completion = new CompletableFuture<>();
        TokenStream stream = service.repairVueProject(prompt);
        ToolExecutionContextHolder.set(ToolExecutionContext.builder()
                .taskId(taskId)
                .appId(appId)
                .userId(userId)
                .callSource(ToolCallSourceEnum.REPAIR)
                .build());
        try {
            stream.onPartialResponse(response::append)
                    .onCompleteResponse(ignored -> completion.complete(null))
                    .onError(completion::completeExceptionally)
                    .start();
            completion.get(repairProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } finally {
            ToolExecutionContextHolder.clear(appId);
        }
    }

    private String buildPrompt(int attempt, BuildDiagnosis diagnosis, BuildResult failedBuild) {
        return """
                Repair attempt: %d
                Suspected files: %s

                Error summary:
                %s

                Build output:
                %s
                """.formatted(attempt, diagnosis.getSuspectedFiles(), diagnosis.getSummary(),
                StrUtil.subPre(failedBuild.getLogText(), repairProperties.getMaxBuildLogChars()));
    }
}
