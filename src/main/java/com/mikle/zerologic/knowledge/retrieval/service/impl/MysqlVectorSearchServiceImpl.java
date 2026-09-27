package com.mikle.zerologic.knowledge.retrieval.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mikle.zerologic.knowledge.document.model.entity.KnowledgeChunk;
import com.mikle.zerologic.knowledge.document.model.entity.KnowledgeDocument;
import com.mikle.zerologic.knowledge.document.model.enums.KnowledgeDocumentStatusEnum;
import com.mikle.zerologic.knowledge.document.service.KnowledgeChunkService;
import com.mikle.zerologic.knowledge.document.service.KnowledgeDocumentService;
import com.mikle.zerologic.knowledge.embedding.model.entity.KnowledgeEmbedding;
import com.mikle.zerologic.knowledge.embedding.service.EmbeddingService;
import com.mikle.zerologic.knowledge.embedding.service.KnowledgeEmbeddingService;
import com.mikle.zerologic.knowledge.embedding.support.EmbeddingJsonUtils;
import com.mikle.zerologic.knowledge.retrieval.model.result.RagRetrievedChunk;
import com.mikle.zerologic.knowledge.retrieval.service.VectorSearchService;
import com.mikle.zerologic.knowledge.retrieval.support.VectorMathUtils;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MysqlVectorSearchServiceImpl implements VectorSearchService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 20;

    @Value("${rag.min-score:0.3}")
    private double minScore;

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
        if (isInvalidId(appId) || isInvalidId(userId) || StrUtil.isBlank(query)
                || (attachmentId != null && isInvalidId(attachmentId))) {
            return List.of();
        }

        int actualTopK = Math.clamp(topK == null ? DEFAULT_TOP_K : topK, 1, MAX_TOP_K);
        String embeddingModel = embeddingService.getModelName();
        int embeddingDimension = embeddingService.getDimension();
        if (StrUtil.isBlank(embeddingModel) || embeddingDimension <= 0) {
            log.warn("Embedding 配置无效，model={}, dimension={}", embeddingModel, embeddingDimension);
            return List.of();
        }

        Map<Long, KnowledgeDocument> documentMap = loadCandidateDocuments(appId, userId, attachmentId);
        if (documentMap.isEmpty()) {
            if (attachmentId != null) {
                log.warn("当前附件未找到可用知识文档，appId={}, userId={}, attachmentId={}",
                        appId, userId, attachmentId);
            }
            return List.of();
        }

        List<Long> candidateChunkIds = loadCandidateChunkIds(
                appId, userId, documentMap.keySet().stream().toList());
        if (candidateChunkIds.isEmpty()) {
            return List.of();
        }

        List<KnowledgeEmbedding> embeddings = loadCandidateEmbeddings(
                appId,
                userId,
                candidateChunkIds,
                embeddingModel,
                embeddingDimension
        );
        if (embeddings.isEmpty()) {
            return List.of();
        }

        List<Double> embeddedQuery = embeddingService.embed(query);
        if (embeddedQuery == null || embeddedQuery.size() != embeddingDimension) {
            log.warn("查询向量维度不符合配置，expected={}, actual={}",
                    embeddingDimension, embeddedQuery == null ? null : embeddedQuery.size());
            return List.of();
        }

        List<ScoredEmbedding> scoredEmbeddings = embeddings.stream()
                .map(embedding -> score(embedding, embeddedQuery))
                .filter(Objects::nonNull)
                .filter(item -> item.score() >= minScore)
                .sorted(Comparator.comparingDouble(ScoredEmbedding::score).reversed())
                .limit(actualTopK)
                .toList();
        if (scoredEmbeddings.isEmpty()) {
            return List.of();
        }

        List<Long> topChunkIds = scoredEmbeddings.stream()
                .map(ScoredEmbedding::chunkId)
                .toList();
        Map<Long, KnowledgeChunk> topChunkMap = loadTopChunks(appId, userId, topChunkIds);

        return scoredEmbeddings.stream()
                .map(item -> toResult(
                        item,
                        topChunkMap.get(item.chunkId()),
                        documentMap
                ))
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<Long, KnowledgeDocument> loadCandidateDocuments(Long appId, Long userId,
                                                                 Long attachmentId) {
        QueryWrapper query = QueryWrapper.create()
                .select("id", "documentName")
                .eq("appId", appId)
                .eq("userId", userId)
                .eq("status", KnowledgeDocumentStatusEnum.ACTIVE.getValue());
        if (attachmentId != null) {
            query.eq("attachmentId", attachmentId);
        }
        return knowledgeDocumentService.list(query).stream()
                .collect(Collectors.toMap(KnowledgeDocument::getId, Function.identity()));
    }

    private List<Long> loadCandidateChunkIds(Long appId, Long userId,
                                             List<Long> documentIds) {
        if (documentIds.isEmpty()) {
            return List.of();
        }
        QueryWrapper query = QueryWrapper.create()
                .select("id")
                .eq("appId", appId)
                .eq("userId", userId)
                .in("documentId", documentIds);
        return knowledgeChunkService.list(query).stream()
                .map(KnowledgeChunk::getId)
                .toList();
    }

    private Map<Long, KnowledgeChunk> loadTopChunks(Long appId, Long userId,
                                                     List<Long> chunkIds) {
        if (chunkIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper query = QueryWrapper.create()
                .select("id", "documentId", "chunkIndex", "content")
                .eq("appId", appId)
                .eq("userId", userId)
                .in("id", chunkIds);
        return knowledgeChunkService.list(query).stream()
                .collect(Collectors.toMap(KnowledgeChunk::getId, Function.identity()));
    }

    private List<KnowledgeEmbedding> loadCandidateEmbeddings(Long appId, Long userId,
                                                              List<Long> chunkIds,
                                                              String embeddingModel,
                                                              int embeddingDimension) {
        if (chunkIds.isEmpty()) {
            return List.of();
        }
        QueryWrapper query = QueryWrapper.create()
                .select("id", "chunkId", "embeddingJson")
                .eq("appId", appId)
                .eq("userId", userId)
                .eq("embeddingModel", embeddingModel)
                .eq("embeddingDimension", embeddingDimension)
                .in("chunkId", chunkIds);
        return knowledgeEmbeddingService.list(query);
    }

    private ScoredEmbedding score(KnowledgeEmbedding embedding,
                                   List<Double> embeddedQuery) {
        try {
            List<Double> vector = EmbeddingJsonUtils.fromJson(embedding.getEmbeddingJson());
            if (vector.size() != embeddedQuery.size()) {
                log.warn("跳过维度不一致的知识向量，embeddingId={}, expected={}, actual={}",
                        embedding.getId(), embeddedQuery.size(), vector.size());
                return null;
            }
            return new ScoredEmbedding(
                    embedding.getChunkId(),
                    VectorMathUtils.cosineSimilarity(embeddedQuery, vector)
            );
        } catch (RuntimeException e) {
            log.warn("跳过无法解析的知识向量，embeddingId={}", embedding.getId(), e);
            return null;
        }
    }

    private RagRetrievedChunk toResult(ScoredEmbedding item,
                                       KnowledgeChunk chunk,
                                       Map<Long, KnowledgeDocument> documentMap) {
        if (chunk == null) {
            return null;
        }
        KnowledgeDocument document = documentMap.get(chunk.getDocumentId());
        if (document == null) {
            return null;
        }
        return RagRetrievedChunk.builder()
                .documentId(document.getId())
                .chunkId(chunk.getId())
                .documentName(document.getDocumentName())
                .chunkIndex(chunk.getChunkIndex())
                .content(chunk.getContent())
                .score(item.score())
                .build();
    }

    private boolean isInvalidId(Long id) {
        return id == null || id <= 0;
    }

    private record ScoredEmbedding(Long chunkId, double score) {
    }
}
