package com.mikle.zerologic.service;

import com.mikle.zerologic.model.dto.generationtask.GenerationTaskCreateRequest;
import com.mikle.zerologic.model.entity.GenerationTask;
import com.mikle.zerologic.model.entity.User;
import com.mikle.zerologic.model.vo.GenerationTaskVO;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

public interface GenerationTaskService extends IService<GenerationTask> {

    Long createGenerateTask(GenerationTaskCreateRequest request, User loginUser);

    GenerationTaskVO getTaskVO(Long taskId, User loginUser);

    Flux<String> streamGenerateTask(Long taskId, User loginUser);

    Boolean cancelTask(Long taskId, User loginUser);
}
