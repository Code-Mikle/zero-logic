package com.mikle.zerologic.workflow.generation.node;

import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.rag.RagService;
import com.mikle.zerologic.rag.model.RagResult;
import com.mikle.zerologic.workflow.generation.GenerationWorkflowContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 生成前检索知识库
 */
@Component
@Slf4j
public class RagRetrieveNode {

    @Resource
    private RagService ragService;

    @Value("${rag.enabled:true}")
    private boolean ragEnabled;

    public AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            GenerationWorkflowContext context = GenerationWorkflowContext.getContext(state);

            if (context == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成工作流上下文不存在");
            }

            if (!ragEnabled) {
                context.setRagContext("");
                context.setRagReferences(List.of());
                context.setCurrentStep("rag_retrieve_skipped");
                return GenerationWorkflowContext.saveContext(context);
            }

            RagResult ragResult;
            try {
                ragResult = ragService.retrieve(
                        context.getTaskId(),
                        context.getAppId(),
                        context.getUserId(),
                        context.getAttachmentId(),
                        context.getOriginalMessage()
                );
            } catch (Exception e) {
                log.warn("RAG 检索失败，降级为无知识库生成，appId={}, taskId={}",
                        context.getAppId(), context.getTaskId(), e);
                context.setRagContext("");
                context.setRagReferences(List.of());
                context.setCurrentStep("rag_retrieve_degraded");
                return GenerationWorkflowContext.saveContext(context);
            }

            context.setRagContext(ragResult.getContextText());
            context.setRagReferences(ragResult.getReferences());
            context.setCurrentStep("rag_retrieve");

            log.info("RAG 检索完成，appId={}, taskId={}, hitCount={}",
                    context.getAppId(),
                    context.getTaskId(),
                    ragResult.getReferences() == null ? 0 : ragResult.getReferences().size());

            return GenerationWorkflowContext.saveContext(context);
        });
    }
}
