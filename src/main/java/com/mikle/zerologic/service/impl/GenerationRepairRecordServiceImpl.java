package com.mikle.zerologic.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mikle.zerologic.core.build.model.BuildDiagnosis;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.exception.ThrowUtils;
import com.mikle.zerologic.mapper.GenerationRepairRecordMapper;
import com.mikle.zerologic.model.entity.GenerationRepairRecord;
import com.mikle.zerologic.model.vo.GenerationRepairRecordVO;
import com.mikle.zerologic.service.GenerationRepairRecordService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenerationRepairRecordServiceImpl
        extends ServiceImpl<GenerationRepairRecordMapper, GenerationRepairRecord>
        implements GenerationRepairRecordService {

    @Override
    public GenerationRepairRecord createRunning(Long taskId, Long appId, Long userId, int repairAttempt,
                                                Long sourceBuildRecordId, BuildDiagnosis diagnosis) {
        GenerationRepairRecord record = GenerationRepairRecord.builder()
                .taskId(taskId).appId(appId).userId(userId).repairAttempt(repairAttempt)
                .sourceBuildRecordId(sourceBuildRecordId).status("running")
                .errorSummary(diagnosis.getSummary())
                .suspectedFiles(JSONUtil.toJsonStr(diagnosis.getSuspectedFiles()))
                .durationMs(0L).build();
        ThrowUtils.throwIf(!save(record) || record.getId() == null,
                ErrorCode.OPERATION_ERROR, "Failed to create repair record");
        return record;
    }

    @Override
    public void finish(Long id, String status, List<String> changedFiles, String aiResponse,
                       String errorMessage, long durationMs) {
        GenerationRepairRecord update = GenerationRepairRecord.builder().id(id).status(status)
                .changedFiles(JSONUtil.toJsonStr(changedFiles == null ? List.of() : changedFiles))
                .aiResponse(StrUtil.subPre(aiResponse, 12000))
                .errorMessage(StrUtil.subPre(errorMessage, 2048)).durationMs(durationMs).build();
        ThrowUtils.throwIf(!updateById(update), ErrorCode.OPERATION_ERROR, "Failed to update repair record");
    }

    @Override
    public List<GenerationRepairRecordVO> listByTaskId(Long taskId) {
        return list(QueryWrapper.create().eq("taskId", taskId).orderBy("repairAttempt", true))
                .stream().map(this::toVO).toList();
    }

    private GenerationRepairRecordVO toVO(GenerationRepairRecord record) {
        GenerationRepairRecordVO vo = new GenerationRepairRecordVO();
        BeanUtil.copyProperties(record, vo);
        vo.setSuspectedFiles(parseList(record.getSuspectedFiles()));
        vo.setChangedFiles(parseList(record.getChangedFiles()));
        return vo;
    }

    private List<String> parseList(String json) {
        return StrUtil.isBlank(json) ? List.of() : JSONUtil.toList(json, String.class);
    }
}
