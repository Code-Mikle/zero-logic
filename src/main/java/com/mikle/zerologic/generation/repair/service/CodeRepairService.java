package com.mikle.zerologic.generation.repair.service;

import com.mikle.zerologic.generation.build.model.result.BuildDiagnosis;
import com.mikle.zerologic.generation.build.model.result.BuildResult;
import com.mikle.zerologic.generation.repair.model.result.CodeRepairResult;

import java.nio.file.Path;

public interface CodeRepairService {
    CodeRepairResult repair(Long taskId, Long appId, Long userId, int repairAttempt,
                            Path projectPath, BuildResult failedBuild, BuildDiagnosis diagnosis);
}
