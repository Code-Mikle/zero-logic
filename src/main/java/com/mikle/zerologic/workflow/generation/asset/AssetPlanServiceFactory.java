package com.mikle.zerologic.workflow.generation.asset;

import com.mikle.zerologic.config.AssetProperties;
import com.mikle.zerologic.config.RoutingAiModelConfig;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssetPlanServiceFactory {

    @Resource
    private RoutingAiModelConfig routingAiModelConfig;

    @Resource
    private AssetProperties assetProperties;

    @Bean
    public AssetPlanService assetPlanService() {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(routingAiModelConfig.getApiKey())
                .modelName(routingAiModelConfig.getModelName())
                .baseUrl(routingAiModelConfig.getBaseUrl())
                .maxTokens(assetProperties.getPlanMaxTokens())
                .temperature(assetProperties.getPlanTemperature())
                .logRequests(routingAiModelConfig.getLogRequests())
                .logResponses(routingAiModelConfig.getLogResponses())
                .build();
        return AiServices.builder(AssetPlanService.class)
                .chatModel(chatModel)
                .build();
    }
}
