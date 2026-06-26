package com.mikle.zerologic.service;

import com.mikle.zerologic.core.build.model.BuildDiagnosis;
import com.mikle.zerologic.model.entity.GenerationRepairRecord;
import com.mikle.zerologic.model.vo.GenerationRepairRecordVO;
import com.mybatisflex.core.service.IService;

import java.util.List;

public interface GenerationRepairRecordService extends IService<GenerationRepairRecord> {
    GenerationRepairRecord createRunning(Long taskId, Long appId, Long userId, int repairAttempt,
                                         Long sourceBuildRecordId, BuildDiagnosis diagnosis);
    void finish(Long id, String status, List<String> changedFiles, String aiResponse,
                String errorMessage, long durationMs);
    List<GenerationRepairRecordVO> listByTaskId(Long taskId);
}
