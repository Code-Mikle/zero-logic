package com.mikle.zerologic.core.version;

import com.mikle.zerologic.core.build.model.BuildResult;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.model.entity.ProjectVersion;
import com.mikle.zerologic.model.enums.CodeGenTypeEnum;
import com.mikle.zerologic.service.ProjectVersionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Set;

@Slf4j
@Component
public class ProjectVersionArchiver {

    private static final Set<String> IGNORED_SOURCE_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".DS_Store",
            ".env", "target", ".mvn", ".idea", ".vscode", "coverage"
    );

    @Resource
    private ProjectVersionService projectVersionService;

    @Resource
    private ProjectVersionPathResolver pathResolver;

    public ProjectVersion archiveBuiltVersion(Long appId, Long userId, Long taskId,
                                              CodeGenTypeEnum codeGenType, Path projectPath,
                                              BuildResult buildResult) {
        if (buildResult == null || !Boolean.TRUE.equals(buildResult.getSuccess())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "构建未通过，无法归档版本");
        }
        Path normalizedProjectPath = projectPath.toAbsolutePath().normalize();
        Path sourceArtifactPath = resolveArtifactSourcePath(codeGenType, normalizedProjectPath, buildResult);
        Integer nextVersionNo = nextVersionNo(appId);
        Path sourceDir = pathResolver.resolveSourceDir(appId, nextVersionNo);
        Path artifactDir = pathResolver.resolveArtifactDir(appId, nextVersionNo);
        try {
            ensureVersionDirectoryAvailable(sourceDir.getParent());
            copyDirectory(normalizedProjectPath, sourceDir, true);
            copyDirectory(sourceArtifactPath, artifactDir, false);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "归档项目版本失败：" + e.getMessage());
        }
        ProjectVersion version = projectVersionService.createBuiltVersion(
                appId,
                userId,
                taskId,
                nextVersionNo,
                codeGenType.getValue(),
                sourceDir.toString(),
                artifactDir.toString(),
                buildResult.getBuildRecordId()
        );
        log.info("项目版本归档完成，appId={}, taskId={}, versionId={}, versionNo={}",
                appId, taskId, version.getId(), version.getVersionNo());
        return version;
    }

    private Integer nextVersionNo(Long appId) {
        ProjectVersion latest = projectVersionService.getOne(com.mybatisflex.core.query.QueryWrapper.create()
                .eq("appId", appId)
                .orderBy("versionNo", false)
                .limit(1));
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    private Path resolveArtifactSourcePath(CodeGenTypeEnum codeGenType, Path projectPath, BuildResult buildResult) {
        Path artifactPath = buildResult.getArtifactPath() == null
                ? null : Path.of(buildResult.getArtifactPath()).toAbsolutePath().normalize();
        if (codeGenType == CodeGenTypeEnum.VUE_PROJECT) {
            if (artifactPath == null || !Files.isDirectory(artifactPath)
                    || !Files.isRegularFile(artifactPath.resolve("index.html"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Vue 构建产物不存在，无法归档版本");
            }
            return artifactPath;
        }
        return artifactPath == null ? projectPath : artifactPath;
    }

    private void ensureVersionDirectoryAvailable(Path versionDir) throws IOException {
        if (Files.exists(versionDir)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "版本目录已存在，无法覆盖：" + versionDir);
        }
        Files.createDirectories(versionDir);
    }

    private void copyDirectory(Path source, Path target, boolean filterGeneratedNoise) throws IOException {
        if (!Files.isDirectory(source)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "源目录不存在：" + source);
        }
        Files.createDirectories(target);
        try (var paths = Files.walk(source)) {
            for (Path path : paths.sorted(Comparator.naturalOrder()).toList()) {
                Path relative = source.relativize(path);
                if (relative.toString().isBlank()) {
                    continue;
                }
                if (filterGeneratedNoise && shouldIgnore(relative)) {
                    continue;
                }
                Path dest = target.resolve(relative).normalize();
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private boolean shouldIgnore(Path relative) {
        for (Path part : relative) {
            if (IGNORED_SOURCE_NAMES.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }
}
