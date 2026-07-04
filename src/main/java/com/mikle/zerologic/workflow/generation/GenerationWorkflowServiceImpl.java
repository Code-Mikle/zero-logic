package com.mikle.zerologic.workflow.generation;

import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.service.GenerationAppLockService;
import com.mikle.zerologic.workflow.generation.node.CodeGenerateNode;
import com.mikle.zerologic.workflow.generation.node.AssetCollectNode;
import com.mikle.zerologic.workflow.generation.node.AssetPlanNode;
import com.mikle.zerologic.workflow.generation.node.BuildCheckNode;
import com.mikle.zerologic.workflow.generation.node.ErrorAnalyzeNode;
import com.mikle.zerologic.workflow.generation.node.AutoRepairNode;
import com.mikle.zerologic.config.RepairProperties;
import com.mikle.zerologic.workflow.generation.node.PrepareContextNode;
import com.mikle.zerologic.workflow.generation.node.PromptAssembleNode;
import com.mikle.zerologic.workflow.generation.node.RagRetrieveNode;
import com.mikle.zerologic.workflow.generation.node.VersionArchiveNode;
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

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

    @Resource
    private AssetPlanNode assetPlanNode;

    @Resource
    private AssetCollectNode assetCollectNode;

    @Resource
    private BuildCheckNode buildCheckNode;

    @Resource
    private ErrorAnalyzeNode errorAnalyzeNode;

    @Resource
    private AutoRepairNode autoRepairNode;

    @Resource
    private VersionArchiveNode versionArchiveNode;

    @Resource
    private RepairProperties repairProperties;

    @Override
    public Flux<String> streamGenerate(GenerationWorkflowRequest request) {
        GenerationWorkflowContext initialContext = GenerationWorkflowContext.fromRequest(request);
        AtomicReference<Flux<String>> codeStreamRef = new AtomicReference<>();
        AtomicReference<GenerationWorkflowContext> contextRef = new AtomicReference<>(initialContext);

        CompiledGraph<MessagesState<String>> workflow = createPreGenerationWorkflow(codeStreamRef);

        try {
            if (log.isDebugEnabled()) {
                GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                log.debug("生成工作流图:\n{}", graph.content());
            }

            int stepCounter = 1;
            String lastLoggedStep = null;

            for (NodeOutput<MessagesState<String>> step : workflow.stream(
                    Map.of(GenerationWorkflowContext.CONTEXT_KEY, initialContext))) {

                GenerationWorkflowContext currentContext =
                        GenerationWorkflowContext.getContext(step.state());

                if (currentContext != null) {
                    contextRef.set(currentContext);
                    String currentStep = currentContext.getCurrentStep();
                    if (!Objects.equals(lastLoggedStep, currentStep)) {
                        log.info("生成工作流第 {} 步完成: appId={}, currentStep={}",
                                stepCounter,
                                currentContext.getAppId(),
                                currentStep);
                        lastLoggedStep = currentStep;
                        stepCounter++;
                    }
                }

            }
        } catch (Exception e) {
            log.error("生成工作流执行失败, appId={}", request.appId(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成工作流执行失败：" + e.getMessage());
        }

        Flux<String> codeStream = codeStreamRef.get();

        if (codeStream == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成工作流未返回代码流");
        }

        return codeStream.concatWith(Flux.defer(() -> {
            GenerationWorkflowContext context = contextRef.get();
            runPostGenerationWorkflow(context);
            if (context.getBuildResult() == null
                    || !Boolean.TRUE.equals(context.getBuildResult().getSuccess())) {
                return Flux.error(new BusinessException(
                        ErrorCode.OPERATION_ERROR, "项目构建失败，请查看任务构建日志"));
            }
            return Flux.empty();
        }));
    }

    private CompiledGraph<MessagesState<String>> createPreGenerationWorkflow(
            AtomicReference<Flux<String>> codeStreamRef) {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("prepare_context", prepareContextNode.create())
                    .addNode("rag_retrieve", ragRetrieveNode.create())
                    .addNode("asset_plan", assetPlanNode.create())
                    .addNode("asset_collect", assetCollectNode.create())
                    .addNode("prompt_assemble", promptAssembleNode.create())
                    .addNode("code_generate", codeGenerateNode.create(codeStreamRef))
                    .addEdge(START, "prepare_context")
                    .addEdge("prepare_context", "rag_retrieve")
                    .addEdge("rag_retrieve", "asset_plan")
                    .addEdge("asset_plan", "asset_collect")
                    .addEdge("asset_collect", "prompt_assemble")
                    .addEdge("prompt_assemble", "code_generate")
                    .addEdge("code_generate", END)
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成工作流创建失败");
        }
    }

    private void runPostGenerationWorkflow(GenerationWorkflowContext context) {
        try {
            CompiledGraph<MessagesState<String>> workflow = createPostGenerationWorkflow();

            for (NodeOutput<MessagesState<String>> step : workflow.stream(
                    Map.of(GenerationWorkflowContext.CONTEXT_KEY, context))) {

                GenerationWorkflowContext currentContext = GenerationWorkflowContext.getContext(step.state());

                if (currentContext != null) {
                    context.setCurrentStep(currentContext.getCurrentStep());
                    context.setGeneratedProjectDir(currentContext.getGeneratedProjectDir());
                    context.setBuildResult(currentContext.getBuildResult());
                    context.setBuildDiagnosis(currentContext.getBuildDiagnosis());
                    context.setRepairAttempt(currentContext.getRepairAttempt());
                    context.setRepairResult(currentContext.getRepairResult());
                    context.setVersionId(currentContext.getVersionId());
                    context.setVersionNo(currentContext.getVersionNo());
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成后置工作流执行失败，taskId={}", context.getTaskId(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "生成后置工作流执行失败：" + e.getMessage());
        }
    }

    private CompiledGraph<MessagesState<String>> createPostGenerationWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("build_check", buildCheckNode.create())
                    .addNode("error_analyze", errorAnalyzeNode.create())
                    .addNode("auto_repair", autoRepairNode.create())
                    .addNode("version_archive", versionArchiveNode.create())
                    .addEdge(START, "build_check")
                    .addConditionalEdges("build_check", edge_async(this::routeAfterBuild), Map.of(
                            "success", "version_archive",
                            "repair", "error_analyze",
                            "failed", END))
                    .addEdge("version_archive", END)
                    .addEdge("error_analyze", "auto_repair")
                    .addConditionalEdges("auto_repair", edge_async(this::routeAfterRepair), Map.of(
                            "rebuild", "build_check",
                            "failed", END))
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成后置工作流创建失败");
        }
    }

    private String routeAfterBuild(MessagesState<String> state) {
        GenerationWorkflowContext context = GenerationWorkflowContext.getContext(state);
        if (context != null && context.getBuildResult() != null
                && Boolean.TRUE.equals(context.getBuildResult().getSuccess())) {
            return "success";
        }
        int attempts = context == null || context.getRepairAttempt() == null ? 0 : context.getRepairAttempt();
        if (context != null && repairProperties.isEnabled()
                && attempts < repairProperties.getMaxAttempts()
                && context.getCodeGenType() == com.mikle.zerologic.model.enums.CodeGenTypeEnum.VUE_PROJECT
                && context.getBuildResult() != null
                && !Boolean.TRUE.equals(context.getBuildResult().getTimedOut())
                && context.getBuildResult().getCommand() != null
                && context.getBuildResult().getCommand().contains(" run build")) {
            return "repair";
        }
        return "failed";
    }

    private String routeAfterRepair(MessagesState<String> state) {
        GenerationWorkflowContext context = GenerationWorkflowContext.getContext(state);
        return context != null && context.getRepairResult() != null
                && context.getRepairResult().isSuccess() ? "rebuild" : "failed";
    }
}
