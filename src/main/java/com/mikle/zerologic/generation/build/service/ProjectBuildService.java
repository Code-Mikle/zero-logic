package com.mikle.zerologic.generation.build.service;

import com.mikle.zerologic.generation.build.model.result.BuildResult;
import com.mikle.zerologic.generation.codegen.model.enums.CodeGenTypeEnum;

import java.nio.file.Path;

public interface ProjectBuildService {
    BuildResult build(Long taskId, Long appId, Long userId,
                      CodeGenTypeEnum codeGenType, Path projectPath, int attemptNo);
}
