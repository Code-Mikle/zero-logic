package com.mikle.zerologic.service.impl;

import com.mikle.zerologic.mapper.GenerationTaskMapper;
import com.mikle.zerologic.model.entity.GenerationTask;
import com.mikle.zerologic.service.GenerationTaskProgressService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GenerationTaskProgressServiceImpl
        extends ServiceImpl<GenerationTaskMapper, GenerationTask>
        implements GenerationTaskProgressService {

    @Override
    public void updateStep(Long taskId, String currentStep) {
        if (taskId == null) {
            return;
        }
        boolean updated = updateById(GenerationTask.builder()
                .id(taskId)
                .currentStep(currentStep)
                .build());
        if (!updated) {
            log.warn("生成任务步骤更新失败，taskId={}, currentStep={}", taskId, currentStep);
        }
    }
}
