package com.mikle.zerologic.generation.codegen.service;

import com.mikle.zerologic.generation.codegen.guardrail.PromptSafetyInputGuardrail;
import com.mikle.zerologic.generation.tool.execution.ToolManager;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.generation.codegen.model.enums.CodeGenTypeEnum;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Resource(name = "streamingChatModelPrototype")
    private StreamingChatModel openAiStreamingChatModel;

    @Resource(name = "reasoningStreamingChatModelPrototype")
    private StreamingChatModel reasoningStreamingChatModel;

    @Resource
    private ToolManager toolManager;

    public AiCodeGeneratorService createAiCodeGeneratorService(CodeGenTypeEnum codeGenType,
                                                                ChatMemory chatMemory) {
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        if (chatMemory == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "对话记忆不能为空");
        }
        return switch (codeGenType) {
            case HTML, MULTI_FILE -> createSimpleCodeService(chatMemory);
            case VUE_PROJECT -> createVueProjectService(chatMemory);
            default -> throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    private AiCodeGeneratorService createVueProjectService(ChatMemory chatMemory) {
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(reasoningStreamingChatModel)
                .chatMemory(chatMemory)
                .tools((Object[]) toolManager.getAllTools())
                .hallucinatedToolNameStrategy(toolExecutionRequest ->
                        ToolExecutionResultMessage.from(toolExecutionRequest,
                                "Error: there is no tool called " + toolExecutionRequest.name()))
                .maxToolCallingRoundTrips(20)
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }

    private AiCodeGeneratorService createSimpleCodeService(ChatMemory chatMemory) {
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(openAiStreamingChatModel)
                .chatMemory(chatMemory)
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }
}
