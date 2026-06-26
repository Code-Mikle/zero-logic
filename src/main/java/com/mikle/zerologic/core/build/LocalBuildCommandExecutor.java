package com.mikle.zerologic.core.build;

import com.mikle.zerologic.config.BuildProperties;
import com.mikle.zerologic.constant.AppConstant;
import com.mikle.zerologic.core.build.model.CommandResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class LocalBuildCommandExecutor implements BuildCommandExecutor {

    @Resource
    private BuildProperties buildProperties;

    @Override
    public CommandResult execute(Path workingDirectory, List<String> command, Duration timeout) {
        long startTime = System.currentTimeMillis();
        Process process = null;
        try {
            Path safeDirectory = validateWorkingDirectory(workingDirectory);
            ProcessBuilder processBuilder = new ProcessBuilder(command)
                    .directory(safeDirectory.toFile())
                    .redirectErrorStream(true);
            processBuilder.environment().put("CI", "true");
            process = processBuilder.start();

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Process runningProcess = process;
                Future<String> outputFuture = executor.submit(
                        () -> readOutput(runningProcess, buildProperties.getMaxLogChars()));
                boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (!finished) {
                    destroyProcessTree(process);
                }
                String output = outputFuture.get(10, TimeUnit.SECONDS);
                return CommandResult.builder()
                        .command(List.copyOf(command))
                        .exitCode(finished ? process.exitValue() : null)
                        .output(output)
                        .durationMs(System.currentTimeMillis() - startTime)
                        .timedOut(!finished)
                        .build();
            }
        } catch (Exception e) {
            if (process != null && process.isAlive()) {
                destroyProcessTree(process);
            }
            log.error("构建命令执行异常，command={}", command, e);
            return CommandResult.builder()
                    .command(List.copyOf(command))
                    .exitCode(-1)
                    .output("命令执行异常：" + e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .timedOut(false)
                    .build();
        }
    }

    private Path validateWorkingDirectory(Path workingDirectory) throws Exception {
        Path root = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR).toAbsolutePath().normalize();
        Path target = workingDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(root) || !Files.isDirectory(target)) {
            throw new IllegalArgumentException("构建目录不存在或超出代码输出目录");
        }
        Path realRoot = root.toRealPath();
        Path realTarget = target.toRealPath();
        if (!realTarget.startsWith(realRoot)) {
            throw new IllegalArgumentException("构建目录不存在或超出代码输出目录");
        }
        return realTarget;
    }

    private String readOutput(Process process, int maxChars) throws Exception {
        StringBuilder output = new StringBuilder(Math.min(maxChars, 8192));
        boolean truncated = false;
        try (InputStreamReader reader = new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8)) {
            char[] buffer = new char[2048];
            int length;
            while ((length = reader.read(buffer)) >= 0) {
                int remaining = maxChars - output.length();
                if (remaining > 0) {
                    output.append(buffer, 0, Math.min(length, remaining));
                }
                if (length > remaining) {
                    truncated = true;
                }
            }
        }
        if (truncated) {
            output.append("\n...[日志已截断]");
        }
        return output.toString();
    }

    private void destroyProcessTree(Process process) {
        process.descendants().forEach(handle -> {
            if (handle.isAlive()) {
                handle.destroyForcibly();
            }
        });
        process.destroyForcibly();
    }
}
