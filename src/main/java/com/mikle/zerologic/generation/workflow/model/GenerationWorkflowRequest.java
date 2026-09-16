package com.mikle.zerologic.generation.workflow.model;

import com.mikle.zerologic.generation.codegen.model.enums.CodeGenTypeEnum;

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
