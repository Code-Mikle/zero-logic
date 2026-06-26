package com.mikle.zerologic.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.exception.ThrowUtils;
import com.mikle.zerologic.mapper.DeployRecordMapper;
import com.mikle.zerologic.model.entity.DeployRecord;
import com.mikle.zerologic.model.enums.DeployRecordStatusEnum;
import com.mikle.zerologic.model.vo.DeployRecordVO;
import com.mikle.zerologic.service.DeployRecordService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeployRecordServiceImpl
        extends ServiceImpl<DeployRecordMapper, DeployRecord>
        implements DeployRecordService {

    @Override
    public DeployRecord createRunning(Long appId, Long userId, Long versionId,
                                      String deployKey, String deployPath, String deployType) {
        DeployRecord record = DeployRecord.builder()
                .appId(appId)
                .userId(userId)
                .versionId(versionId)
                .deployKey(deployKey)
                .deployPath(deployPath)
                .deployType(deployType)
                .status(DeployRecordStatusEnum.RUNNING.getValue())
                .build();
        ThrowUtils.throwIf(!save(record) || record.getId() == null,
                ErrorCode.OPERATION_ERROR, "创建部署记录失败");
        return record;
    }

    @Override
    public void finishSuccess(Long recordId, String deployUrl) {
        DeployRecord update = DeployRecord.builder()
                .id(recordId)
                .deployUrl(deployUrl)
                .status(DeployRecordStatusEnum.SUCCESS.getValue())
                .build();
        ThrowUtils.throwIf(!updateById(update), ErrorCode.OPERATION_ERROR, "更新部署记录失败");
    }

    @Override
    public void finishFailed(Long recordId, String errorMessage) {
        DeployRecord update = DeployRecord.builder()
                .id(recordId)
                .status(DeployRecordStatusEnum.FAILED.getValue())
                .errorMessage(StrUtil.subPre(errorMessage, 2048))
                .build();
        ThrowUtils.throwIf(!updateById(update), ErrorCode.OPERATION_ERROR, "更新部署失败记录失败");
    }

    @Override
    public List<DeployRecordVO> listByAppId(Long appId, Long userId) {
        if (appId == null || userId == null) {
            return List.of();
        }
        return list(QueryWrapper.create()
                .eq("appId", appId)
                .eq("userId", userId)
                .orderBy("id", false))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private DeployRecordVO toVO(DeployRecord record) {
        DeployRecordVO vo = new DeployRecordVO();
        BeanUtil.copyProperties(record, vo);
        return vo;
    }
}
