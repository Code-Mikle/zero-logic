package com.mikle.zerologic.service.impl;

import com.mikle.zerologic.config.BuildProperties;
import com.mikle.zerologic.core.build.model.BuildResult;
import com.mikle.zerologic.core.builder.VueProjectBuilder;
import com.mikle.zerologic.model.entity.GenerationBuildRecord;
import com.mikle.zerologic.model.enums.CodeGenTypeEnum;
import com.mikle.zerologic.model.enums.GenerationBuildStatusEnum;
import com.mikle.zerologic.service.GenerationBuildRecordService;
import com.mikle.zerologic.service.ProjectBuildService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ProjectBuildServiceImpl implements ProjectBuildService {

    @Resource
    private GenerationBuildRecordService generationBuildRecordService;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private BuildProperties buildProperties;

    @Override
    public BuildResult build(Long taskId, Long appId, Long userId,
                             CodeGenTypeEnum codeGenType, Path projectPath, int attemptNo) {
        GenerationBuildRecord record = generationBuildRecordService.createRunning(
                taskId, appId, userId, attemptNo, codeGenType.getValue(), projectPath.toString());
        BuildResult result;
        try {
            if (!buildProperties.isEnabled()) {
                result = success(projectPath, "构建检查已关闭", "build-check-disabled");
            } else {
                result = switch (codeGenType) {
                    case HTML -> validateHtml(projectPath);
                    case MULTI_FILE -> validateMultiFile(projectPath);
                    case VUE_PROJECT -> vueProjectBuilder.buildProjectWithResult(projectPath.toString());
                };
            }
        } catch (RuntimeException e) {
            result = failed(projectPath, "构建检查异常：" + e.getMessage(), "build-check");
        }
        result.setBuildRecordId(record.getId());
        result.setProjectPath(projectPath.toString());
        generationBuildRecordService.finish(record.getId(), result);
        return result;
    }

    private BuildResult validateHtml(Path projectPath) {
        return validateRequiredFile(projectPath, "index.html", "html-static-check");
    }

    private BuildResult validateMultiFile(Path projectPath) {
        BuildResult htmlResult = validateRequiredFile(projectPath, "index.html", "multi-file-static-check");
        if (!Boolean.TRUE.equals(htmlResult.getSuccess())) {
            return htmlResult;
        }
        return success(projectPath, "index.html 检查通过；CSS 和 JavaScript 文件为可选文件",
                "multi-file-static-check");
    }

    private BuildResult validateRequiredFile(Path projectPath, String fileName, String command) {
        long startTime = System.currentTimeMillis();
        Path target = projectPath.resolve(fileName).normalize();
        try {
            if (!target.startsWith(projectPath) || !Files.isRegularFile(target) || Files.size(target) == 0) {
                return failed(projectPath, fileName + " 不存在或内容为空", command);
            }
            BuildResult result = success(projectPath, fileName + " 检查通过", command);
            result.setDurationMs(System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            return failed(projectPath, fileName + " 检查失败：" + e.getMessage(), command);
        }
    }

    private BuildResult success(Path projectPath, String logText, String command) {
        return BuildResult.builder()
                .success(true)
                .status(GenerationBuildStatusEnum.SUCCESS.getValue())
                .command(command)
                .exitCode(0)
                .logText(logText)
                .durationMs(0L)
                .timedOut(false)
                .projectPath(projectPath.toString())
                .artifactPath(projectPath.toString())
                .build();
    }

    private BuildResult failed(Path projectPath, String logText, String command) {
        return BuildResult.builder()
                .success(false)
                .status(GenerationBuildStatusEnum.FAILED.getValue())
                .command(command)
                .exitCode(-1)
                .logText(logText)
                .durationMs(0L)
                .timedOut(false)
                .projectPath(projectPath.toString())
                .build();
    }
}
