package com.mikle.zerologic.generation.build.service;

import com.mikle.zerologic.generation.build.model.result.BuildResult;
import com.mikle.zerologic.generation.build.model.entity.GenerationBuildRecord;
import com.mybatisflex.core.service.IService;

public interface GenerationBuildRecordService extends IService<GenerationBuildRecord> {
    GenerationBuildRecord createRunning(Long taskId, Long appId, Long userId,
                                        Integer attemptNo, String codeGenType,
                                        String projectPath);

    void finish(Long recordId, BuildResult result);

    GenerationBuildRecord getLatestByTaskId(Long taskId);
}
