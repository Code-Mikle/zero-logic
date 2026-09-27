package com.mikle.zerologic.generation.workflow.model;

import com.mikle.zerologic.generation.codegen.model.enums.CodeGenTypeEnum;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class GenerationWorkflowContextTest {

    @Test
    void shouldKeepTaskScopedMemoriesWhenCreatingContext() {
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        ChatMemory repairMemory = MessageWindowChatMemory.withMaxMessages(20);
        GenerationWorkflowRequest request = new GenerationWorkflowRequest(
                1L, 2L, 3L, "model message", "display message",
                CodeGenTypeEnum.VUE_PROJECT, null, chatMemory, repairMemory
        );

        GenerationWorkflowContext context = GenerationWorkflowContext.fromRequest(request);

        assertSame(chatMemory, context.getChatMemory());
        assertSame(repairMemory, context.getRepairMemory());
    }
}
