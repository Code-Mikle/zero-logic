package com.mikle.zerologic.knowledge.retrieval.service;

import com.mikle.zerologic.knowledge.retrieval.model.vo.RagRetrievalVO;

import java.util.Collection;
import java.util.Map;

public interface RagRetrievalLogQueryService {

    RagRetrievalVO getByTaskId(Long taskId, Long appId, Long userId);

    Map<Long, RagRetrievalVO> listByTaskIds(
            Collection<Long> taskIds, Long appId, Long userId);

}
