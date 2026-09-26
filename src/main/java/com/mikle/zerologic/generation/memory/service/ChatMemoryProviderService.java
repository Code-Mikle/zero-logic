package com.mikle.zerologic.generation.memory.service;

import cn.hutool.core.convert.Convert;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mikle.zerologic.conversation.service.ChatHistoryService;
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

    private static final String APP_CACHE_KEY_PREFIX = "app:";

    private static final String REPAIR_MEMORY_ID_PREFIX = "repair:";

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    private final Cache<String, MessageWindowChatMemory> memoryCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) ->
                    log.debug("Chat memory removed, appId={}, cause={}", key, cause))
            .build();

    public MessageWindowChatMemory getMemory(Object memoryId) {
        if (memoryId instanceof Number number) {
            return getMemory(number.longValue());
        }
        String memoryIdText = Convert.toStr(memoryId);
        if (memoryIdText != null && memoryIdText.startsWith(REPAIR_MEMORY_ID_PREFIX)) {
            Long appId = Convert.toLong(memoryIdText.substring(REPAIR_MEMORY_ID_PREFIX.length()));
            if (appId == null || appId <= 0 || !memoryIdText.equals(getRepairMemoryId(appId))) {
                throw new IllegalArgumentException("memoryId must be a valid repair memory id");
            }
            return memoryCache.get(memoryIdText, key -> createMemory(memoryIdText));
        }
        Long appId = Convert.toLong(memoryId);
        if (appId == null) {
            throw new IllegalArgumentException("memoryId must be a valid appId");
        }
        return getMemory(appId);
    }

    public MessageWindowChatMemory getMemory(Long appId) {
        validateAppId(appId);
        return memoryCache.get(APP_CACHE_KEY_PREFIX + appId, key -> createAppMemory(appId));
    }

    public String getRepairMemoryId(Long appId) {
        validateAppId(appId);
        return REPAIR_MEMORY_ID_PREFIX + appId;
    }

    /**
     * 清理应用的普通生成记忆和自动修复记忆。
     */
    public void clearMemory(Long appId) {
        validateAppId(appId);
        String appCacheKey = APP_CACHE_KEY_PREFIX + appId;
        String repairMemoryId = getRepairMemoryId(appId);
        RuntimeException failure = null;
        try {
            redisChatMemoryStore.deleteMessages(appId);
        } catch (RuntimeException e) {
            failure = e;
        }
        try {
            redisChatMemoryStore.deleteMessages(repairMemoryId);
        } catch (RuntimeException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        } finally {
            memoryCache.invalidate(appCacheKey);
            memoryCache.invalidate(repairMemoryId);
        }
        if (failure != null) {
            throw failure;
        }
    }

    private MessageWindowChatMemory createAppMemory(Long appId) {
        MessageWindowChatMemory chatMemory = createMemory(appId);
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, MAX_MESSAGES);
        return chatMemory;
    }

    private MessageWindowChatMemory createMemory(Object memoryId) {
        return MessageWindowChatMemory.builder()
                .id(memoryId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(MAX_MESSAGES)
                .build();
    }

    private void validateAppId(Long appId) {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException("appId must be positive");
        }
    }
}
