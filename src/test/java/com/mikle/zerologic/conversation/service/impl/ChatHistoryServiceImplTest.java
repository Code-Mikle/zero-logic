package com.mikle.zerologic.conversation.service.impl;

import com.mikle.zerologic.conversation.model.dto.ChatHistoryQueryRequest;
import com.mikle.zerologic.conversation.model.entity.ChatHistory;
import com.mikle.zerologic.conversation.model.enums.ChatHistoryMessageTypeEnum;
import com.mikle.zerologic.exception.BusinessException;
import com.mybatisflex.core.query.QueryWrapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class ChatHistoryServiceImplTest {

    private final ChatHistoryServiceImpl chatHistoryService = new ChatHistoryServiceImpl();

    @Test
    void getQueryWrapperShouldRejectUnsupportedMessageType() {
        ChatHistoryQueryRequest request = new ChatHistoryQueryRequest();
        request.setMessageType("assistant");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatHistoryService.getQueryWrapper(request)
        );

        assertEquals("不支持的消息类型", exception.getMessage());
    }

    @Test
    void getQueryWrapperShouldRejectUnsupportedSortField() {
        ChatHistoryQueryRequest request = new ChatHistoryQueryRequest();
        request.setSortField("id desc; drop table chat_history");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatHistoryService.getQueryWrapper(request)
        );

        assertEquals("不支持的排序字段", exception.getMessage());
    }

    @Test
    void getQueryWrapperShouldRejectUnsupportedSortDirection() {
        ChatHistoryQueryRequest request = new ChatHistoryQueryRequest();
        request.setSortOrder("random");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatHistoryService.getQueryWrapper(request)
        );

        assertEquals("不支持的排序方向", exception.getMessage());
    }

    @Test
    void getQueryWrapperShouldUseStableDefaultSortAndIgnoreBlankFilters() {
        ChatHistoryQueryRequest request = new ChatHistoryQueryRequest();
        request.setMessage("  ");
        request.setMessageType("");

        String sql = chatHistoryService.getQueryWrapper(request).toSQL().toLowerCase();
        int orderByIndex = sql.indexOf("order by");
        String orderByClause = orderByIndex < 0 ? "" : sql.substring(orderByIndex);

        assertFalse(sql.contains("message like"));
        assertFalse(sql.contains("messagetype ="));
        assertTrue(orderByClause.contains("createtime"));
        assertTrue(orderByClause.contains("id"));
        assertTrue(orderByClause.indexOf("createtime") < orderByClause.indexOf("id"));
    }

    @Test
    void loadChatHistoryToMemoryShouldRestoreSupportedMessagesInChronologicalOrder() {
        ChatHistoryServiceImpl service = spy(new ChatHistoryServiceImpl());
        LocalDateTime now = LocalDateTime.now();
        ChatHistory oldestUserMessage = ChatHistory.builder()
                .id(1L)
                .appId(10L)
                .message("old user message")
                .messageType(ChatHistoryMessageTypeEnum.USER.getValue())
                .createTime(now.minusMinutes(2))
                .build();
        ChatHistory unsupportedMessage = ChatHistory.builder()
                .id(2L)
                .appId(10L)
                .message("unsupported")
                .messageType("system")
                .createTime(now.minusMinutes(1))
                .build();
        ChatHistory newestAiMessage = ChatHistory.builder()
                .id(3L)
                .appId(10L)
                .message("new ai message")
                .messageType(ChatHistoryMessageTypeEnum.AI.getValue())
                .createTime(now)
                .build();
        doReturn(List.of(newestAiMessage, unsupportedMessage, oldestUserMessage))
                .when(service).list(any(QueryWrapper.class));
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();

        int loadedCount = service.loadChatHistoryToMemory(10L, memory, 20);

        assertEquals(2, loadedCount);
        assertEquals(2, memory.messages().size());
        assertInstanceOf(UserMessage.class, memory.messages().get(0));
        assertInstanceOf(AiMessage.class, memory.messages().get(1));

        ArgumentCaptor<QueryWrapper> queryCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        org.mockito.Mockito.verify(service).list(queryCaptor.capture());
        String sql = queryCaptor.getValue().toSQL().toLowerCase();
        int orderByIndex = sql.indexOf("order by");
        String orderByClause = orderByIndex < 0 ? "" : sql.substring(orderByIndex);
        assertTrue(orderByClause.contains("createtime"));
        assertTrue(orderByClause.contains("id"));
        assertTrue(orderByClause.indexOf("createtime") < orderByClause.indexOf("id"));
    }

    @Test
    void loadChatHistoryToMemoryShouldClearStaleMemoryWhenDatabaseIsEmpty() {
        ChatHistoryServiceImpl service = spy(new ChatHistoryServiceImpl());
        doReturn(List.of()).when(service).list(any(QueryWrapper.class));
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();
        memory.add(UserMessage.from("stale message"));

        int loadedCount = service.loadChatHistoryToMemory(10L, memory, 20);

        assertEquals(0, loadedCount);
        assertTrue(memory.messages().isEmpty());
    }

    @Test
    void loadChatHistoryToMemoryShouldRejectInvalidArguments() {
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();

        assertEquals("应用ID不能为空", assertThrows(
                BusinessException.class,
                () -> chatHistoryService.loadChatHistoryToMemory(null, memory, 20)
        ).getMessage());
        assertEquals("聊天记忆不能为空", assertThrows(
                BusinessException.class,
                () -> chatHistoryService.loadChatHistoryToMemory(10L, null, 20)
        ).getMessage());
        assertEquals("加载消息数量必须大于 0", assertThrows(
                BusinessException.class,
                () -> chatHistoryService.loadChatHistoryToMemory(10L, memory, 0)
        ).getMessage());
    }
}
