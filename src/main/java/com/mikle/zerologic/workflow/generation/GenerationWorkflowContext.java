package com.mikle.zerologic.workflow.generation;

import com.mikle.zerologic.model.enums.CodeGenTypeEnum;
import com.mikle.zerologic.core.build.model.BuildResult;
import com.mikle.zerologic.core.build.model.BuildDiagnosis;
import com.mikle.zerologic.core.repair.model.CodeRepairResult;
import com.mikle.zerologic.rag.model.RagRetrievedChunk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationWorkflowContext implements Serializable {

    public static final String CONTEXT_KEY = "generationWorkflowContext";

    @Serial
    private static final long serialVersionUID = 1L;

    private Long taskId;

    private Long appId;

    private Long userId;

    // 真正传给模型的初始消息
    private String message;
    // 用户原始展示消息
    private String displayMessage;
    // 生成类型，比如 HTML、多文件、Vue 项目
    private CodeGenTypeEnum codeGenType;

    private Long attachmentId;
    // 当前 workflow 步骤，初始为 init
    private String currentStep;
    // 原始用户消息，用于 RAG 检索和 prompt 展示
    private String originalMessage;

    private String ragContext;

    private List<RagRetrievedChunk> ragReferences;
    // 最终传给模型的 prompt，初始等于 message
    private String assembledMessage;

    private String generatedProjectDir;
    // 构建次数，初始为 1
    private Integer buildAttempt;

    private BuildResult buildResult;

    private BuildDiagnosis buildDiagnosis;
    // 自动修复次数，初始为 0
    private Integer repairAttempt;

    private CodeRepairResult repairResult;

    private Long versionId;

    private Integer versionNo;

    public static GenerationWorkflowContext fromRequest(GenerationWorkflowRequest request) {
        return GenerationWorkflowContext.builder()
                .taskId(request.taskId())
                .appId(request.appId())
                .userId(request.userId())
                .message(request.message())
                .displayMessage(request.displayMessage())
                .codeGenType(request.codeGenType())
                .attachmentId(request.attachmentId())
                .currentStep("init")
                .buildAttempt(1)
                .repairAttempt(0)
                .originalMessage(request.displayMessage())
                .assembledMessage(request.message())
                .build();
    }

    public static GenerationWorkflowContext getContext(MessagesState<String> state) {
        return (GenerationWorkflowContext) state.data().get(CONTEXT_KEY);
    }

    public static Map<String, Object> saveContext(GenerationWorkflowContext context) {
        return Map.of(CONTEXT_KEY, context);
    }
}
