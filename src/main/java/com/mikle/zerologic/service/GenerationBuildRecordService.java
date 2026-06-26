package com.mikle.zerologic.service;

import com.mikle.zerologic.core.build.model.BuildResult;
import com.mikle.zerologic.model.entity.GenerationBuildRecord;
import com.mybatisflex.core.service.IService;

public interface GenerationBuildRecordService extends IService<GenerationBuildRecord> {
    GenerationBuildRecord createRunning(Long taskId, Long appId, Long userId,
                                        Integer attemptNo, String codeGenType,
                                        String projectPath);

    void finish(Long recordId, BuildResult result);

    GenerationBuildRecord getLatestByTaskId(Long taskId);
}
