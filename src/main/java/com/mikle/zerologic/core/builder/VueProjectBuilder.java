package com.mikle.zerologic.core.builder;

import com.mikle.zerologic.config.BuildProperties;
import com.mikle.zerologic.core.build.BuildCommandExecutor;
import com.mikle.zerologic.core.build.model.BuildResult;
import com.mikle.zerologic.core.build.model.CommandResult;
import com.mikle.zerologic.model.enums.GenerationBuildStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class VueProjectBuilder {

    @Resource
    private BuildCommandExecutor buildCommandExecutor;

    @Resource
    private BuildProperties buildProperties;

    public boolean buildProject(String projectPath) {
        return Boolean.TRUE.equals(buildProjectWithResult(projectPath).getSuccess());
    }

    public void buildProjectAsync(String projectPath) {
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis())
                .start(() -> buildProject(projectPath));
    }

    public BuildResult buildProjectWithResult(String projectPath) {
        long startTime = System.currentTimeMillis();
        Path projectDir = Path.of(projectPath).toAbsolutePath().normalize();
        Path packageJson = projectDir.resolve("package.json");
        if (!Files.isRegularFile(packageJson)) {
            return failed(projectDir, "未找到 package.json", null, startTime);
        }

        Path distDir = projectDir.resolve("dist").normalize();
        try {
            deleteDirectory(distDir);
        } catch (IOException e) {
            return failed(projectDir, "清理旧 dist 目录失败：" + e.getMessage(), null, startTime);
        }

        List<String> installCommand = Files.isRegularFile(projectDir.resolve("package-lock.json"))
                ? List.of(npmCommand(), "ci", "--no-audit", "--no-fund")
                : List.of(npmCommand(), "install", "--no-audit", "--no-fund");
        CommandResult installResult = buildCommandExecutor.execute(
                projectDir, installCommand, Duration.ofSeconds(buildProperties.getInstallTimeoutSeconds()));
        if (!commandSucceeded(installResult)) {
            return fromCommand(projectDir, installResult, null, startTime);
        }

        List<String> buildCommand = List.of(npmCommand(), "run", "build");
        CommandResult buildResult = buildCommandExecutor.execute(
                projectDir, buildCommand, Duration.ofSeconds(buildProperties.getBuildTimeoutSeconds()));
        String combinedLog = "[依赖安装]\n" + installResult.getOutput()
                + "\n\n[项目构建]\n" + buildResult.getOutput();
        BuildResult result = fromCommand(projectDir, buildResult, combinedLog, startTime);
        if (Boolean.TRUE.equals(result.getSuccess())
                && !Files.isRegularFile(distDir.resolve("index.html"))) {
            result.setSuccess(false);
            result.setStatus(GenerationBuildStatusEnum.FAILED.getValue());
            result.setLogText(combinedLog + "\n构建命令成功，但未生成 dist/index.html");
            result.setArtifactPath(null);
        } else if (Boolean.TRUE.equals(result.getSuccess())) {
            result.setArtifactPath(distDir.toString());
        }
        result.setCommand(String.join(" ", installCommand) + " && " + String.join(" ", buildCommand));
        return result;
    }

    private BuildResult fromCommand(Path projectDir, CommandResult commandResult,
                                    String overrideLog, long startTime) {
        boolean timedOut = Boolean.TRUE.equals(commandResult.getTimedOut());
        boolean success = commandSucceeded(commandResult);
        return BuildResult.builder()
                .success(success)
                .status(timedOut ? GenerationBuildStatusEnum.TIMEOUT.getValue()
                        : success ? GenerationBuildStatusEnum.SUCCESS.getValue()
                        : GenerationBuildStatusEnum.FAILED.getValue())
                .command(String.join(" ", commandResult.getCommand()))
                .exitCode(commandResult.getExitCode())
                .logText(overrideLog == null ? commandResult.getOutput() : overrideLog)
                .durationMs(System.currentTimeMillis() - startTime)
                .timedOut(timedOut)
                .projectPath(projectDir.toString())
                .build();
    }

    private BuildResult failed(Path projectDir, String message,
                               Integer exitCode, long startTime) {
        return BuildResult.builder()
                .success(false)
                .status(GenerationBuildStatusEnum.FAILED.getValue())
                .command("project-validation")
                .exitCode(exitCode)
                .logText(message)
                .durationMs(System.currentTimeMillis() - startTime)
                .timedOut(false)
                .projectPath(projectDir.toString())
                .build();
    }

    private boolean commandSucceeded(CommandResult result) {
        return !Boolean.TRUE.equals(result.getTimedOut())
                && Integer.valueOf(0).equals(result.getExitCode());
    }

    private String npmCommand() {
        return System.getProperty("os.name").toLowerCase().contains("windows") ? "npm.cmd" : "npm";
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
