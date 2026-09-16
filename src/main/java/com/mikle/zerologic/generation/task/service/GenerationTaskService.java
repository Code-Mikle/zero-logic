package com.mikle.zerologic.generation.task.service;

import com.mikle.zerologic.generation.task.model.dto.GenerationTaskCreateRequest;
import com.mikle.zerologic.generation.task.model.entity.GenerationTask;
import com.mikle.zerologic.user.model.entity.User;
import com.mikle.zerologic.generation.task.model.vo.GenerationTaskVO;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

public interface GenerationTaskService extends IService<GenerationTask> {

    Long createGenerateTask(GenerationTaskCreateRequest request, User loginUser);

    GenerationTaskVO getTaskVO(Long taskId, User loginUser);

    Flux<String> streamGenerateTask(Long taskId, User loginUser);

    Boolean cancelTask(Long taskId, User loginUser);
}
