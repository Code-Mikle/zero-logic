package com.mikle.zerologic.knowledge.retrieval.service.impl;

import com.mikle.zerologic.knowledge.document.model.entity.KnowledgeChunk;
import com.mikle.zerologic.knowledge.document.model.entity.KnowledgeDocument;
import com.mikle.zerologic.knowledge.document.service.KnowledgeChunkService;
import com.mikle.zerologic.knowledge.document.service.KnowledgeDocumentService;
import com.mikle.zerologic.knowledge.embedding.model.entity.KnowledgeEmbedding;
import com.mikle.zerologic.knowledge.embedding.service.EmbeddingService;
import com.mikle.zerologic.knowledge.embedding.service.KnowledgeEmbeddingService;
import com.mikle.zerologic.knowledge.embedding.support.EmbeddingJsonUtils;
import com.mikle.zerologic.knowledge.retrieval.model.result.RagRetrievedChunk;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MysqlVectorSearchServiceImplTest {

    private EmbeddingService embeddingService;
    private KnowledgeEmbeddingService knowledgeEmbeddingService;
    private KnowledgeChunkService knowledgeChunkService;
    private KnowledgeDocumentService knowledgeDocumentService;
    private MysqlVectorSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        embeddingService = mock(EmbeddingService.class);
        knowledgeEmbeddingService = mock(KnowledgeEmbeddingService.class);
        knowledgeChunkService = mock(KnowledgeChunkService.class);
        knowledgeDocumentService = mock(KnowledgeDocumentService.class);
        service = new MysqlVectorSearchServiceImpl();
        ReflectionTestUtils.setField(service, "embeddingService", embeddingService);
        ReflectionTestUtils.setField(service, "knowledgeEmbeddingService", knowledgeEmbeddingService);
        ReflectionTestUtils.setField(service, "knowledgeChunkService", knowledgeChunkService);
        ReflectionTestUtils.setField(service, "knowledgeDocumentService", knowledgeDocumentService);
        ReflectionTestUtils.setField(service, "minScore", 0.3D);
        when(embeddingService.getModelName()).thenReturn("test-model");
        when(embeddingService.getDimension()).thenReturn(2);
    }

    @Test
    void shouldNotCallEmbeddingApiWhenAttachmentHasNoActiveDocument() {
        when(knowledgeDocumentService.list(any(QueryWrapper.class))).thenReturn(List.of());

        List<RagRetrievedChunk> result = service.search(1L, 2L, 3L, "query", 5);

        assertTrue(result.isEmpty());
        verify(embeddingService, never()).embed(any());
        verify(knowledgeChunkService, never()).list(any(QueryWrapper.class));
        verify(knowledgeEmbeddingService, never()).list(any(QueryWrapper.class));

        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(knowledgeDocumentService).list(captor.capture());
        String sql = captor.getValue().toSQL().toLowerCase();
        assertTrue(sql.contains("attachmentid"));
        assertTrue(sql.contains("status"));
    }

    @Test
    void shouldFilterCandidatesBeforeEmbeddingAndOnlyLoadTopChunkContent() {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id(10L)
                .documentName("requirements.md")
                .build();
        KnowledgeChunk metadata = KnowledgeChunk.builder()
                .id(100L)
                .documentId(10L)
                .build();
        KnowledgeChunk fullChunk = KnowledgeChunk.builder()
                .id(100L)
                .documentId(10L)
                .chunkIndex(0)
                .content("payment requirements")
                .build();
        KnowledgeEmbedding embedding = KnowledgeEmbedding.builder()
                .id(1000L)
                .chunkId(100L)
                .embeddingJson(EmbeddingJsonUtils.toJson(List.of(1D, 0D)))
                .build();
        when(knowledgeDocumentService.list(any(QueryWrapper.class)))
                .thenReturn(List.of(document));
        when(knowledgeChunkService.list(any(QueryWrapper.class)))
                .thenReturn(List.of(metadata), List.of(fullChunk));
        when(knowledgeEmbeddingService.list(any(QueryWrapper.class)))
                .thenReturn(List.of(embedding));
        when(embeddingService.embed("query")).thenReturn(List.of(1D, 0D));

        List<RagRetrievedChunk> result = service.search(1L, 2L, 3L, "query", 5);

        assertEquals(1, result.size());
        assertEquals(10L, result.getFirst().getDocumentId());
        assertEquals(100L, result.getFirst().getChunkId());
        assertEquals("requirements.md", result.getFirst().getDocumentName());
        assertEquals("payment requirements", result.getFirst().getContent());
        assertEquals(1D, result.getFirst().getScore(), 0.000001D);

        InOrder order = inOrder(
                knowledgeDocumentService,
                knowledgeChunkService,
                knowledgeEmbeddingService,
                embeddingService
        );
        order.verify(knowledgeDocumentService).list(any(QueryWrapper.class));
        order.verify(knowledgeChunkService).list(any(QueryWrapper.class));
        order.verify(knowledgeEmbeddingService).list(any(QueryWrapper.class));
        order.verify(embeddingService).embed("query");
        order.verify(knowledgeChunkService).list(any(QueryWrapper.class));

        ArgumentCaptor<QueryWrapper> embeddingQueryCaptor =
                ArgumentCaptor.forClass(QueryWrapper.class);
        verify(knowledgeEmbeddingService).list(embeddingQueryCaptor.capture());
        String embeddingSql = embeddingQueryCaptor.getValue().toSQL().toLowerCase();
        assertTrue(embeddingSql.contains("embeddingmodel"));
        assertTrue(embeddingSql.contains("embeddingdimension"));

        ArgumentCaptor<QueryWrapper> chunkQueryCaptor =
                ArgumentCaptor.forClass(QueryWrapper.class);
        verify(knowledgeChunkService, times(2)).list(chunkQueryCaptor.capture());
        List<QueryWrapper> chunkQueries = chunkQueryCaptor.getAllValues();
        String metadataSql = chunkQueries.get(0).toSQL().toLowerCase();
        String contentSql = chunkQueries.get(1).toSQL().toLowerCase();
        assertTrue(metadataSql.contains("select id"));
        assertFalse(metadataSql.contains("content"));
        assertTrue(contentSql.contains("content"));
    }

    @Test
    void shouldFilterResultsBelowMinimumScoreWithoutLoadingContent() {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id(10L)
                .documentName("requirements.md")
                .build();
        KnowledgeChunk metadata = KnowledgeChunk.builder()
                .id(100L)
                .documentId(10L)
                .build();
        KnowledgeEmbedding embedding = KnowledgeEmbedding.builder()
                .id(1000L)
                .chunkId(100L)
                .embeddingJson(EmbeddingJsonUtils.toJson(List.of(0D, 1D)))
                .build();
        when(knowledgeDocumentService.list(any(QueryWrapper.class)))
                .thenReturn(List.of(document));
        when(knowledgeChunkService.list(any(QueryWrapper.class)))
                .thenReturn(List.of(metadata));
        when(knowledgeEmbeddingService.list(any(QueryWrapper.class)))
                .thenReturn(List.of(embedding));
        when(embeddingService.embed("query")).thenReturn(List.of(1D, 0D));

        List<RagRetrievedChunk> result = service.search(1L, 2L, null, "query", 5);

        assertTrue(result.isEmpty());
        verify(knowledgeChunkService, times(1)).list(any(QueryWrapper.class));
    }

    @Test
    void shouldNotCallEmbeddingApiWhenDocumentHasNoChunks() {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id(10L)
                .documentName("requirements.md")
                .build();
        when(knowledgeDocumentService.list(any(QueryWrapper.class)))
                .thenReturn(List.of(document));
        when(knowledgeChunkService.list(any(QueryWrapper.class))).thenReturn(List.of());

        List<RagRetrievedChunk> result = service.search(1L, 2L, null, "query", 5);

        assertTrue(result.isEmpty());
        verify(knowledgeEmbeddingService, never()).list(any(QueryWrapper.class));
        verify(embeddingService, never()).embed(any());
    }
}
