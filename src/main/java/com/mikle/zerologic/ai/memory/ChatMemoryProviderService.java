package com.mikle.zerologic.ai.memory;

import cn.hutool.core.convert.Convert;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mikle.zerologic.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class ChatMemoryProviderService {

    private static final int MAX_MESSAGES = 20;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    private final Cache<Long, MessageWindowChatMemory> memoryCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) ->
                    log.debug("Chat memory removed, appId={}, cause={}", key, cause))
            .build();

    public MessageWindowChatMemory getMemory(Object memoryId) {
        Long appId = Convert.toLong(memoryId);
        if (appId == null) {
            throw new IllegalArgumentException("memoryId must be a valid appId");
        }
        return getMemory(appId);
    }

    public MessageWindowChatMemory getMemory(Long appId) {
        if (appId == null) {
            throw new IllegalArgumentException("appId must not be null");
        }
        return memoryCache.get(appId, this::createMemory);
    }

    private MessageWindowChatMemory createMemory(Long appId) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(MAX_MESSAGES)
                .build();
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, MAX_MESSAGES);
        return chatMemory;
    }
}
