package com.mikle.zerologic.generation.memory.service;

import com.mikle.zerologic.conversation.service.ChatHistoryService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ChatMemoryProviderService {

    private static final int MAX_MESSAGES = 20;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 为单次生成任务创建内存，并从数据库恢复本轮之前的最近消息。
     */
    public MessageWindowChatMemory createAppMemory(Long appId) {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException("appId must be positive");
        }
        MessageWindowChatMemory chatMemory = createEmptyMemory();
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, MAX_MESSAGES);
        return chatMemory;
    }

    /**
     * 自动修复使用独立的任务级内存，不读取普通生成对话。
     */
    public MessageWindowChatMemory createRepairMemory() {
        return createEmptyMemory();
    }

    private MessageWindowChatMemory createEmptyMemory() {
        return MessageWindowChatMemory.withMaxMessages(MAX_MESSAGES);
    }
}
