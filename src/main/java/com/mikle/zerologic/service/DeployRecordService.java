package com.mikle.zerologic.service;

import com.mikle.zerologic.model.entity.DeployRecord;
import com.mikle.zerologic.model.vo.DeployRecordVO;
import com.mybatisflex.core.service.IService;

import java.util.List;

public interface DeployRecordService extends IService<DeployRecord> {

    DeployRecord createRunning(Long appId, Long userId, Long versionId,
                               String deployKey, String deployPath, String deployType);

    void finishSuccess(Long recordId, String deployUrl);

    void finishFailed(Long recordId, String errorMessage);

    List<DeployRecordVO> listByAppId(Long appId, Long userId);
}
