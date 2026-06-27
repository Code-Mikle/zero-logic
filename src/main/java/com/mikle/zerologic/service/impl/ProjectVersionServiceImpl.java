package com.mikle.zerologic.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.exception.ThrowUtils;
import com.mikle.zerologic.mapper.ProjectVersionMapper;
import com.mikle.zerologic.model.entity.ProjectVersion;
import com.mikle.zerologic.model.enums.ProjectVersionStatusEnum;
import com.mikle.zerologic.model.vo.ProjectVersionVO;
import com.mikle.zerologic.service.ProjectVersionService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectVersionServiceImpl
        extends ServiceImpl<ProjectVersionMapper, ProjectVersion>
        implements ProjectVersionService {

    @Override
    public ProjectVersion createBuiltVersion(Long appId, Long userId, Long taskId, Integer versionNo, String codeGenType,
                                             String sourcePath, String artifactPath, Long buildRecordId) {
        ProjectVersion version = ProjectVersion.builder()
                .appId(appId)
                .userId(userId)
                .taskId(taskId)
                .versionNo(versionNo)
                .versionName("v" + versionNo)
                .codeGenType(codeGenType)
                .sourcePath(sourcePath)
                .artifactPath(artifactPath)
                .buildRecordId(buildRecordId)
                .status(ProjectVersionStatusEnum.BUILT.getValue())
                .build();
        ThrowUtils.throwIf(!save(version) || version.getId() == null,
                ErrorCode.OPERATION_ERROR, "创建项目版本失败");
        return version;
    }

    @Override
    public ProjectVersion getLatestDeployableVersion(Long appId, Long userId) {
        if (appId == null || userId == null) {
            return null;
        }
        return getOne(QueryWrapper.create()
                .eq("appId", appId)
                .eq("userId", userId)
                .in("status", ProjectVersionStatusEnum.BUILT.getValue(),
                        ProjectVersionStatusEnum.DEPLOYED.getValue())
                .orderBy("versionNo", false)
                .limit(1));
    }

    @Override
    public ProjectVersion getDeployableVersion(Long appId, Long userId, Long versionId) {
        if (appId == null || userId == null || versionId == null) {
            return null;
        }
        return getOne(QueryWrapper.create()
                .eq("id", versionId)
                .eq("appId", appId)
                .eq("userId", userId)
                .in("status", ProjectVersionStatusEnum.BUILT.getValue(),
                        ProjectVersionStatusEnum.DEPLOYED.getValue())
                .limit(1));
    }

    @Override
    public List<ProjectVersion> listAfterVersionNo(Long appId, Long userId, Integer versionNo) {
        if (appId == null || userId == null || versionNo == null) {
            return List.of();
        }
        return list(QueryWrapper.create()
                .eq("appId", appId)
                .eq("userId", userId)
                .gt("versionNo", versionNo)
                .orderBy("versionNo", true));
    }

    @Override
    public int physicalDeleteAfterVersionNo(Long appId, Long userId, Integer versionNo) {
        if (appId == null || userId == null || versionNo == null) {
            return 0;
        }
        return mapper.physicalDeleteAfterVersionNo(appId, userId, versionNo);
    }

    @Override
    public List<ProjectVersionVO> listByAppId(Long appId, Long userId) {
        if (appId == null || userId == null) {
            return List.of();
        }
        return list(QueryWrapper.create()
                .eq("appId", appId)
                .eq("userId", userId)
                .orderBy("versionNo", false))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public void markCurrentDeployed(Long appId, Long userId, Long versionId) {
        if (appId == null || userId == null || versionId == null) {
            return;
        }
        ProjectVersion reset = ProjectVersion.builder()
                .status(ProjectVersionStatusEnum.BUILT.getValue())
                .build();
        update(reset, QueryWrapper.create()
                .eq("appId", appId)
                .eq("userId", userId)
                .eq("status", ProjectVersionStatusEnum.DEPLOYED.getValue())
                .ne("id", versionId));
        ProjectVersion update = ProjectVersion.builder()
                .id(versionId)
                .status(ProjectVersionStatusEnum.DEPLOYED.getValue())
                .build();
        ThrowUtils.throwIf(!updateById(update), ErrorCode.OPERATION_ERROR, "更新项目版本状态失败");
    }

    private ProjectVersionVO toVO(ProjectVersion version) {
        ProjectVersionVO vo = new ProjectVersionVO();
        BeanUtil.copyProperties(version, vo);
        return vo;
    }
}
