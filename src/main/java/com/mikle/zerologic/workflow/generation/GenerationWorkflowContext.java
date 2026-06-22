package com.mikle.zerologic.workflow.generation;

import com.mikle.zerologic.model.enums.CodeGenTypeEnum;
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

    private String message;

    private String displayMessage;

    private CodeGenTypeEnum codeGenType;

    private Long attachmentId;

    private String currentStep;

    private String originalMessage;

    private String ragContext;

    private List<RagRetrievedChunk> ragReferences;

    private String assembledMessage;

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
