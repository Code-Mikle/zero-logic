package com.mikle.zerologic.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mikle.zerologic.ai.AiCodeGeneratorService;
import com.mikle.zerologic.ai.AiCodeGeneratorServiceFactory;
import com.mikle.zerologic.ai.tools.ToolExecutionContext;
import com.mikle.zerologic.ai.tools.ToolExecutionContextHolder;
import com.mikle.zerologic.config.RepairProperties;
import com.mikle.zerologic.core.build.model.BuildDiagnosis;
import com.mikle.zerologic.core.build.model.BuildResult;
import com.mikle.zerologic.core.repair.ProjectSnapshotService;
import com.mikle.zerologic.core.repair.model.CodeRepairResult;
import com.mikle.zerologic.model.entity.GenerationRepairRecord;
import com.mikle.zerologic.model.enums.CodeGenTypeEnum;
import com.mikle.zerologic.service.CodeRepairService;
import com.mikle.zerologic.service.GenerationRepairRecordService;
import dev.langchain4j.service.TokenStream;
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
                                   Path projectPath, BuildResult failedBuild, BuildDiagnosis diagnosis) {
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
            invokeAgent(taskId, appId, userId, buildPrompt(repairAttempt, diagnosis, failedBuild), response);
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
                             String prompt, StringBuilder response) throws Exception {
        AiCodeGeneratorService service = aiServiceFactory.getAiCodeGeneratorService(appId, CodeGenTypeEnum.VUE_PROJECT);
        CompletableFuture<Void> completion = new CompletableFuture<>();
        TokenStream stream = service.repairVueProject(appId, prompt);
        ToolExecutionContextHolder.set(ToolExecutionContext.builder()
                .taskId(taskId)
                .appId(appId)
                .userId(userId)
                .callSource("repair")
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
