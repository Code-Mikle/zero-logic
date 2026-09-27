package com.mikle.zerologic.generation.memory.service;

import com.mikle.zerologic.conversation.service.ChatHistoryService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatMemoryProviderServiceTest {

    @Mock
    private ChatHistoryService chatHistoryService;

    @InjectMocks
    private ChatMemoryProviderService chatMemoryProviderService;

    @Test
    void appMemoryShouldBeCreatedPerTaskAndHydratedFromDatabase() {
        MessageWindowChatMemory first = chatMemoryProviderService.createAppMemory(12L);
        MessageWindowChatMemory second = chatMemoryProviderService.createAppMemory(12L);

        assertNotSame(first, second);
        verify(chatHistoryService).loadChatHistoryToMemory(12L, first, 20);
        verify(chatHistoryService).loadChatHistoryToMemory(12L, second, 20);
    }

    @Test
    void repairMemoryShouldBeEmptyAndIsolatedFromNormalHistory() {
        MessageWindowChatMemory first = chatMemoryProviderService.createRepairMemory();
        MessageWindowChatMemory second = chatMemoryProviderService.createRepairMemory();

        assertNotSame(first, second);
        assertTrue(first.messages().isEmpty());
        assertTrue(second.messages().isEmpty());
        verify(chatHistoryService, never()).loadChatHistoryToMemory(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(MessageWindowChatMemory.class),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    void appMemoryShouldRejectInvalidAppId() {
        assertThrows(IllegalArgumentException.class,
                () -> chatMemoryProviderService.createAppMemory(0L));
        assertThrows(IllegalArgumentException.class,
                () -> chatMemoryProviderService.createAppMemory(null));
    }
}
