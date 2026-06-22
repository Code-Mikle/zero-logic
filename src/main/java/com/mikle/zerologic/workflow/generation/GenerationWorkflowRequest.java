package com.mikle.zerologic.workflow.generation;

import com.mikle.zerologic.model.enums.CodeGenTypeEnum;

/**
 * 作为 AppServiceImpl -> GenerationWorkflowService 的入参对象。
 */
public record GenerationWorkflowRequest(
        Long taskId,

        Long appId,

        Long userId,

        String message,

        String displayMessage,

        CodeGenTypeEnum codeGenType,

        Long attachmentId
) {
}
