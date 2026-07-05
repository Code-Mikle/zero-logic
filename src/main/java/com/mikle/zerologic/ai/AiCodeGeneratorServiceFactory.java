package com.mikle.zerologic.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mikle.zerologic.ai.guardrail.PromptSafetyInputGuardrail;
import com.mikle.zerologic.ai.memory.ChatMemoryProviderService;
import com.mikle.zerologic.ai.tools.ToolManager;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.model.enums.CodeGenTypeEnum;
import com.mikle.zerologic.utils.SpringContextUtil;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Resource
    private ToolManager toolManager;

    @Resource
    private ChatMemoryProviderService chatMemoryProviderService;

    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(10)
            .expireAfterWrite(Duration.ofHours(2))
            .expireAfterAccess(Duration.ofHours(1))
            .removalListener((key, value, cause) ->
                    log.debug("AI service removed, cacheKey={}, cause={}", key, cause))
            .build();

    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(CodeGenTypeEnum.HTML);
    }

    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        return getAiCodeGeneratorService(codeGenType);
    }

    public AiCodeGeneratorService getAiCodeGeneratorService(CodeGenTypeEnum codeGenType) {
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        String cacheKey = buildCacheKey(codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(codeGenType));
    }

    private AiCodeGeneratorService createAiCodeGeneratorService(CodeGenTypeEnum codeGenType) {
        log.info("Create AI code generator service, codeGenType={}", codeGenType.getValue());
        return switch (codeGenType) {
            case VUE_PROJECT -> createVueProjectService();
            case HTML, MULTI_FILE -> createSimpleCodeService();
            default -> throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    private AiCodeGeneratorService createVueProjectService() {
        StreamingChatModel reasoningStreamingChatModel =
                SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
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
        StreamingChatModel openAiStreamingChatModel =
                SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(openAiStreamingChatModel)
                .chatMemoryProvider(memoryId -> chatMemoryProviderService.getMemory(memoryId))
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }

    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(CodeGenTypeEnum.HTML);
    }

    private String buildCacheKey(CodeGenTypeEnum codeGenType) {
        return codeGenType.getValue();
    }
}
