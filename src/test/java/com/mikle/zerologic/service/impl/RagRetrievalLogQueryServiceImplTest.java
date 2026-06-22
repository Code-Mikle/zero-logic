package com.mikle.zerologic.service.impl;

import com.mikle.zerologic.model.entity.RagRetrievalLog;
import com.mikle.zerologic.model.vo.RagRetrievalVO;
import com.mikle.zerologic.service.RagRetrievalLogService;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagRetrievalLogQueryServiceImplTest {

    @Mock
    private RagRetrievalLogService ragRetrievalLogService;

    @InjectMocks
    private RagRetrievalLogQueryServiceImpl queryService;

    @Test
    void shouldConvertHitChunksAndLimitSnippetLength() {
        String content = "a".repeat(500);
        RagRetrievalLog retrievalLog = RagRetrievalLog.builder()
                .id(1L)
                .taskId(10L)
                .appId(20L)
                .userId(30L)
                .topK(5)
                .hitCount(1)
                .injectedCharLength(500)
                .queryText("生成页面")
                .hitChunksJson("[{\"documentId\":1,\"chunkId\":2,\"documentName\":\"需求.md\","
                        + "\"chunkIndex\":0,\"content\":\"" + content + "\",\"score\":0.86}]")
                .build();
        when(ragRetrievalLogService.getOne(any(QueryWrapper.class))).thenReturn(retrievalLog);

        RagRetrievalVO result = queryService.getByTaskId(10L, 20L, 30L);

        assertNotNull(result);
        assertEquals(1, result.getReferences().size());
        assertEquals(400, result.getReferences().getFirst().getContentSnippet().length());
        assertEquals("需求.md", result.getReferences().getFirst().getDocumentName());
    }
}
