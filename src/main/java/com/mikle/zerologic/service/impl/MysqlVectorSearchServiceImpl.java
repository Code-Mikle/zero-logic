package com.mikle.zerologic.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mikle.zerologic.service.KnowledgeChunkService;
import com.mikle.zerologic.service.KnowledgeDocumentService;
import com.mikle.zerologic.service.KnowledgeEmbeddingService;
import com.mikle.zerologic.model.entity.KnowledgeChunk;
import com.mikle.zerologic.model.entity.KnowledgeDocument;
import com.mikle.zerologic.model.entity.KnowledgeEmbedding;
import com.mikle.zerologic.model.enums.KnowledgeDocumentStatusEnum;
import com.mikle.zerologic.rag.EmbeddingJsonUtils;
import com.mikle.zerologic.rag.EmbeddingService;
import com.mikle.zerologic.rag.VectorMathUtils;
import com.mikle.zerologic.rag.VectorSearchService;
import com.mikle.zerologic.rag.model.RagRetrievedChunk;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MysqlVectorSearchServiceImpl implements VectorSearchService {

    private static final int MAX_TOP_K = 20;

    @Resource
    private EmbeddingService embeddingService;

    @Resource
    private KnowledgeEmbeddingService knowledgeEmbeddingService;

    @Resource
    private KnowledgeChunkService knowledgeChunkService;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Override
    public List<RagRetrievedChunk> search(Long appId, Long userId, Long attachmentId,
                                          String query, Integer topK) {
        if (appId == null || userId == null || StrUtil.isBlank(query)) {
            return List.of();
        }
        int actualTopK = Math.min(Math.max(topK == null ? 5 : topK, 1), MAX_TOP_K);
        List<Double> embeddedQuery = embeddingService.embed(query);

        Long preferredDocumentId = findPreferredDocumentId(appId, userId, attachmentId);
        if (attachmentId != null && preferredDocumentId == null) {
            log.warn("当前附件未找到可用知识文档，appId={}, userId={}, attachmentId={}",
                    appId, userId, attachmentId);
            return List.of();
        }

        QueryWrapper embeddingQuery = QueryWrapper.create()
                .eq("appId", appId)
                .eq("userId", userId);
        List<KnowledgeEmbedding> embeddings = knowledgeEmbeddingService.list(embeddingQuery);
        if (embeddings.isEmpty()) {
            return List.of();
        }

        Map<Long, KnowledgeChunk> chunkMap = knowledgeChunkService.listByIds(
                        embeddings.stream().map(KnowledgeEmbedding::getChunkId).toList())
                .stream()
                .collect(Collectors.toMap(KnowledgeChunk::getId, Function.identity()));

        Long documentIdFilter = preferredDocumentId;
        List<ScoredEmbedding> scoredEmbeddings = embeddings.stream()
                .map(embedding -> score(embedding, chunkMap.get(embedding.getChunkId()), embeddedQuery))
                .filter(Objects::nonNull)
                .filter(item -> documentIdFilter == null
                        || Objects.equals(documentIdFilter, item.chunk().getDocumentId()))
                .sorted(Comparator.comparingDouble(ScoredEmbedding::score).reversed())
                .limit(actualTopK)
                .toList();
        if (scoredEmbeddings.isEmpty()) {
            return List.of();
        }

        Collection<Long> documentIds = scoredEmbeddings.stream()
                .map(item -> item.chunk().getDocumentId())
                .distinct()
                .toList();
        Map<Long, KnowledgeDocument> documentMap = knowledgeDocumentService.listByIds(documentIds)
                .stream()
                .filter(document -> KnowledgeDocumentStatusEnum.ACTIVE.getValue().equals(document.getStatus()))
                .collect(Collectors.toMap(KnowledgeDocument::getId, Function.identity()));

        return scoredEmbeddings.stream()
                .map(item -> toResult(item, documentMap.get(item.chunk().getDocumentId())))
                .filter(Objects::nonNull)
                .toList();
    }

    private Long findPreferredDocumentId(Long appId, Long userId, Long attachmentId) {
        if (attachmentId == null) {
            return null;
        }
        QueryWrapper query = QueryWrapper.create()
                .eq("appId", appId)
                .eq("userId", userId)
                .eq("attachmentId", attachmentId)
                .eq("status", KnowledgeDocumentStatusEnum.ACTIVE.getValue());
        KnowledgeDocument document = knowledgeDocumentService.getOne(query);
        return document == null ? null : document.getId();
    }

    private ScoredEmbedding score(KnowledgeEmbedding embedding, KnowledgeChunk chunk,
                                  List<Double> embeddedQuery) {
        if (chunk == null) {
            return null;
        }
        try {
            List<Double> vector = EmbeddingJsonUtils.fromJson(embedding.getEmbeddingJson());
            if (vector.size() != embeddedQuery.size()) {
                log.warn("跳过维度不一致的知识向量，embeddingId={}, expected={}, actual={}",
                        embedding.getId(), embeddedQuery.size(), vector.size());
                return null;
            }
            return new ScoredEmbedding(chunk,
                    VectorMathUtils.cosineSimilarity(embeddedQuery, vector));
        } catch (RuntimeException e) {
            log.warn("跳过无法解析的知识向量，embeddingId={}", embedding.getId(), e);
            return null;
        }
    }

    private RagRetrievedChunk toResult(ScoredEmbedding item, KnowledgeDocument document) {
        if (document == null) {
            return null;
        }
        KnowledgeChunk chunk = item.chunk();
        return RagRetrievedChunk.builder()
                .documentId(document.getId())
                .chunkId(chunk.getId())
                .documentName(document.getDocumentName())
                .chunkIndex(chunk.getChunkIndex())
                .content(chunk.getContent())
                .score(item.score())
                .build();
    }

    private record ScoredEmbedding(KnowledgeChunk chunk, double score) {
    }
}
