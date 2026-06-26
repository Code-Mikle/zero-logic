package com.mikle.zerologic.ai.tools;

import com.mikle.zerologic.constant.AppConstant;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ProjectToolPathResolver {

    public Path resolve(Long appId, String relativePath) throws IOException {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException("Invalid appId");
        }
        Path input = Path.of(relativePath == null ? "" : relativePath);
        if (input.isAbsolute()) {
            throw new IllegalArgumentException("Absolute paths are not allowed");
        }
        Path root = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + appId)
                .toAbsolutePath().normalize();
        Path target = root.resolve(input).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Path escapes generated project");
        }
        rejectSymlinkPath(root, target);
        return target;
    }

    private void rejectSymlinkPath(Path root, Path target) throws IOException {
        Path current = root;
        if (Files.exists(current) && Files.isSymbolicLink(current)) {
            throw new IllegalArgumentException("Symbolic links are not allowed");
        }
        for (Path part : root.relativize(target)) {
            current = current.resolve(part);
            if (Files.exists(current) && Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("Symbolic links are not allowed");
            }
        }
    }
}
