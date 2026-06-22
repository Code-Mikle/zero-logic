package com.mikle.zerologic.workflow.generation;

import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.service.GenerationAppLockService;
import com.mikle.zerologic.workflow.generation.node.CodeGenerateNode;
import com.mikle.zerologic.workflow.generation.node.PrepareContextNode;
import com.mikle.zerologic.workflow.generation.node.PromptAssembleNode;
import com.mikle.zerologic.workflow.generation.node.RagRetrieveNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

@Slf4j
@Service
public class GenerationWorkflowServiceImpl implements GenerationWorkflowService {

    @Resource
    private PrepareContextNode prepareContextNode;

    @Resource
    private CodeGenerateNode codeGenerateNode;

    @Resource
    private RagRetrieveNode ragRetrieveNode;

    @Resource
    private PromptAssembleNode promptAssembleNode;

    @Override
    public Flux<String> streamGenerate(GenerationWorkflowRequest request) {
        GenerationWorkflowContext initialContext = GenerationWorkflowContext.fromRequest(request);
        AtomicReference<Flux<String>> codeStreamRef = new AtomicReference<>();

        CompiledGraph<MessagesState<String>> workflow = createWorkflow(codeStreamRef);

        try {
            if (log.isDebugEnabled()) {
                GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                log.debug("生成工作流图:\n{}", graph.content());
            }

            int stepCounter = 1;

            for (NodeOutput<MessagesState<String>> step : workflow.stream(
                    Map.of(GenerationWorkflowContext.CONTEXT_KEY, initialContext))) {

                GenerationWorkflowContext currentContext =
                        GenerationWorkflowContext.getContext(step.state());

                if (currentContext != null) {
                    log.info("生成工作流第 {} 步完成: appId={}, currentStep={}",
                            stepCounter,
                            currentContext.getAppId(),
                            currentContext.getCurrentStep());
                }

                stepCounter++;
            }
        } catch (Exception e) {
            log.error("生成工作流执行失败, appId={}", request.appId(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成工作流执行失败：" + e.getMessage());
        }

        Flux<String> codeStream = codeStreamRef.get();

        if (codeStream == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成工作流未返回代码流");
        }

        return codeStream;
    }

    private CompiledGraph<MessagesState<String>> createWorkflow(AtomicReference<Flux<String>> codeStreamRef) {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("prepare_context", prepareContextNode.create())
                    .addNode("rag_retrieve", ragRetrieveNode.create())
                    .addNode("prompt_assemble", promptAssembleNode.create())
                    .addNode("code_generate", codeGenerateNode.create(codeStreamRef))
                    .addEdge(START, "prepare_context")
                    .addEdge("prepare_context", "rag_retrieve")
                    .addEdge("rag_retrieve", "prompt_assemble")
                    .addEdge("prompt_assemble", "code_generate")
                    .addEdge("code_generate", END)
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成工作流创建失败");
        }
    }
}
