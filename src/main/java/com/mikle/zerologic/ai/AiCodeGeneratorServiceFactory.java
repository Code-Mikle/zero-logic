package com.mikle.zerologic.ai;

import com.mikle.zerologic.ai.guardrail.PromptSafetyInputGuardrail;
import com.mikle.zerologic.ai.memory.ChatMemoryProviderService;
import com.mikle.zerologic.ai.tools.ToolManager;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.model.enums.CodeGenTypeEnum;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
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

    @Resource
    private ChatMemoryProviderService chatMemoryProviderService;

    private AiCodeGeneratorService simpleCodeService;

    private AiCodeGeneratorService vueProjectService;

    @PostConstruct
    public void init() {
        this.simpleCodeService = createSimpleCodeService();
        this.vueProjectService = createVueProjectService();
        log.info("AI code generator services initialized");
    }

    public AiCodeGeneratorService getAiCodeGeneratorService(CodeGenTypeEnum codeGenType) {
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        return switch (codeGenType) {
            case HTML, MULTI_FILE -> simpleCodeService;
            case VUE_PROJECT -> vueProjectService;
            default -> throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    private AiCodeGeneratorService createVueProjectService() {
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(reasoningStreamingChatModel)
                .chatMemoryProvider(memoryId -> chatMemoryProviderService.getMemory(memoryId))
                .tools((Object[]) toolManager.getAllTools())
                .hallucinatedToolNameStrategy(toolExecutionRequest ->
                        ToolExecutionResultMessage.from(toolExecutionRequest,
                                "Error: there is no tool called " + toolExecutionRequest.name()))
                .maxSequentialToolsInvocations(20)
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }

    private AiCodeGeneratorService createSimpleCodeService() {
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(openAiStreamingChatModel)
                .chatMemoryProvider(memoryId -> chatMemoryProviderService.getMemory(memoryId))
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }
}
