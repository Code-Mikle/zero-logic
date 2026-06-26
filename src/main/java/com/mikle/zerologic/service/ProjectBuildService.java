package com.mikle.zerologic.service;

import com.mikle.zerologic.core.build.model.BuildResult;
import com.mikle.zerologic.model.enums.CodeGenTypeEnum;

import java.nio.file.Path;

public interface ProjectBuildService {
    BuildResult build(Long taskId, Long appId, Long userId,
                      CodeGenTypeEnum codeGenType, Path projectPath, int attemptNo);
}
