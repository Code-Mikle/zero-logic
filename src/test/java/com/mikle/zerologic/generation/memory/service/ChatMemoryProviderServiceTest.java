package com.mikle.zerologic.generation.memory.service;

import com.mikle.zerologic.conversation.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatMemoryProviderServiceTest {

    @Mock
    private RedisChatMemoryStore redisChatMemoryStore;

    @Mock
    private ChatHistoryService chatHistoryService;

    @InjectMocks
    private ChatMemoryProviderService chatMemoryProviderService;

    @Test
    void appMemoryShouldBeCachedAndHydratedFromChatHistoryOnce() {
        MessageWindowChatMemory first = chatMemoryProviderService.getMemory(12L);
        MessageWindowChatMemory second = chatMemoryProviderService.getMemory(12L);

        assertSame(first, second);
        verify(chatHistoryService).loadChatHistoryToMemory(12L, first, 20);
    }

    @Test
    void repairMemoryShouldBeIsolatedAndShouldNotLoadNormalChatHistory() {
        String repairMemoryId = chatMemoryProviderService.getRepairMemoryId(12L);

        MessageWindowChatMemory repairMemory = chatMemoryProviderService.getMemory(repairMemoryId);
        MessageWindowChatMemory normalMemory = chatMemoryProviderService.getMemory(12L);

        assertNotSame(normalMemory, repairMemory);
        verify(chatHistoryService).loadChatHistoryToMemory(12L, normalMemory, 20);
        verify(chatHistoryService, never()).loadChatHistoryToMemory(12L, repairMemory, 20);
    }

    @Test
    void clearMemoryShouldDeleteNormalAndRepairMemoryAndInvalidateLocalCache() {
        String repairMemoryId = chatMemoryProviderService.getRepairMemoryId(12L);
        MessageWindowChatMemory oldNormalMemory = chatMemoryProviderService.getMemory(12L);
        MessageWindowChatMemory oldRepairMemory = chatMemoryProviderService.getMemory(repairMemoryId);

        chatMemoryProviderService.clearMemory(12L);

        verify(redisChatMemoryStore).deleteMessages(12L);
        verify(redisChatMemoryStore).deleteMessages(repairMemoryId);
        assertNotSame(oldNormalMemory, chatMemoryProviderService.getMemory(12L));
        assertNotSame(oldRepairMemory, chatMemoryProviderService.getMemory(repairMemoryId));
        verify(chatHistoryService, times(2)).loadChatHistoryToMemory(
                org.mockito.ArgumentMatchers.eq(12L),
                org.mockito.ArgumentMatchers.any(MessageWindowChatMemory.class),
                org.mockito.ArgumentMatchers.eq(20)
        );
    }

    @Test
    void memoryIdShouldRejectInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> chatMemoryProviderService.getMemory(0L));
        assertThrows(IllegalArgumentException.class, () -> chatMemoryProviderService.getMemory("repair:invalid"));
        assertThrows(IllegalArgumentException.class, () -> chatMemoryProviderService.getRepairMemoryId(null));
    }
}
