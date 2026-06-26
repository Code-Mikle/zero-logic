package com.mikle.zerologic.service;

import com.mikle.zerologic.core.build.model.BuildDiagnosis;
import com.mikle.zerologic.core.build.model.BuildResult;
import com.mikle.zerologic.core.repair.model.CodeRepairResult;

import java.nio.file.Path;

public interface CodeRepairService {
    CodeRepairResult repair(Long taskId, Long appId, Long userId, int repairAttempt,
                            Path projectPath, BuildResult failedBuild, BuildDiagnosis diagnosis);
}
