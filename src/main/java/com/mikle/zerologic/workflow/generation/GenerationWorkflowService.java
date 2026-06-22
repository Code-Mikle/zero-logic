package com.mikle.zerologic.workflow.generation;

import reactor.core.publisher.Flux;

/**
 * 对业务层暴露工作流入口
 *
 */
public interface GenerationWorkflowService {

    /**
     * 通过 LangGraph4j 工作流生成代码。
     * 阶段 1 保持返回 Flux<String>，避免改前端 SSE 协议。
     * 这个接口就是替代 AiCodeGeneratorFacade.generateAndSaveCodeStream(...) 的入口
     */
    Flux<String> streamGenerate(GenerationWorkflowRequest request);

}
