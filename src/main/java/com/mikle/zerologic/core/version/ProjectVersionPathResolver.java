package com.mikle.zerologic.core.version;

import com.mikle.zerologic.constant.AppConstant;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class ProjectVersionPathResolver {

    public Path resolveVersionDir(Long appId, Integer versionNo) {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException("Invalid appId");
        }
        if (versionNo == null || versionNo <= 0) {
            throw new IllegalArgumentException("Invalid versionNo");
        }
        return Path.of(AppConstant.PROJECT_VERSION_ROOT_DIR, "app_" + appId, "v" + versionNo)
                .toAbsolutePath()
                .normalize();
    }

    public Path resolveSourceDir(Long appId, Integer versionNo) {
        return resolveVersionDir(appId, versionNo).resolve("source").normalize();
    }

    public Path resolveArtifactDir(Long appId, Integer versionNo) {
        return resolveVersionDir(appId, versionNo).resolve("artifact").normalize();
    }
}
