package com.mikle.zerologic.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mikle.zerologic.config.BuildProperties;
import com.mikle.zerologic.core.build.model.BuildResult;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.exception.ThrowUtils;
import com.mikle.zerologic.mapper.GenerationBuildRecordMapper;
import com.mikle.zerologic.model.entity.GenerationBuildRecord;
import com.mikle.zerologic.model.enums.GenerationBuildStatusEnum;
import com.mikle.zerologic.service.GenerationBuildRecordService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class GenerationBuildRecordServiceImpl
        extends ServiceImpl<GenerationBuildRecordMapper, GenerationBuildRecord>
        implements GenerationBuildRecordService {

    @Resource
    private BuildProperties buildProperties;

    @Override
    public GenerationBuildRecord createRunning(Long taskId, Long appId, Long userId,
                                               Integer attemptNo, String codeGenType,
                                               String projectPath) {
        GenerationBuildRecord record = GenerationBuildRecord.builder()
                .taskId(taskId)
                .appId(appId)
                .userId(userId)
                .attemptNo(attemptNo)
                .codeGenType(codeGenType)
                .status(GenerationBuildStatusEnum.RUNNING.getValue())
                .durationMs(0L)
                .timedOut(false)
                .projectPath(projectPath)
                .build();
        boolean saved = save(record);
        ThrowUtils.throwIf(!saved || record.getId() == null,
                ErrorCode.OPERATION_ERROR, "创建构建记录失败");
        return record;
    }

    @Override
    public void finish(Long recordId, BuildResult result) {
        GenerationBuildRecord record = GenerationBuildRecord.builder()
                .id(recordId)
                .status(result.getStatus())
                .command(StrUtil.subPre(result.getCommand(), 1024))
                .exitCode(result.getExitCode())
                .logText(StrUtil.subPre(result.getLogText(), buildProperties.getMaxLogChars()))
                .durationMs(result.getDurationMs())
                .timedOut(Boolean.TRUE.equals(result.getTimedOut()))
                .artifactPath(result.getArtifactPath())
                .build();
        ThrowUtils.throwIf(!updateById(record), ErrorCode.OPERATION_ERROR, "更新构建记录失败");
    }

    @Override
    public GenerationBuildRecord getLatestByTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }
        return getOne(QueryWrapper.create()
                .eq("taskId", taskId)
                .orderBy("attemptNo", false)
                .limit(1));
    }
}
