package com.mikle.zerologic.service;

import com.mikle.zerologic.model.entity.ProjectVersion;
import com.mikle.zerologic.model.vo.ProjectVersionVO;
import com.mybatisflex.core.service.IService;

import java.util.List;

public interface ProjectVersionService extends IService<ProjectVersion> {

    ProjectVersion createBuiltVersion(Long appId, Long userId, Long taskId, Integer versionNo, String codeGenType,
                                      String sourcePath, String artifactPath, Long buildRecordId);

    ProjectVersion getLatestDeployableVersion(Long appId, Long userId);

    ProjectVersion getDeployableVersion(Long appId, Long userId, Long versionId);

    List<ProjectVersionVO> listByAppId(Long appId, Long userId);

    void markDeployed(Long versionId);
}
