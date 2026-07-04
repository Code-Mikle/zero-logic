package com.mikle.zerologic.workflow.generation.asset;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class AssetPlanServiceFactory {

    @Bean
    public AssetPlanService assetPlanService(@Qualifier("routingChatModelPrototype") ChatModel chatModel) {
        return AiServices.builder(AssetPlanService.class)
                .chatModel(chatModel)
                .build();
    }
}
