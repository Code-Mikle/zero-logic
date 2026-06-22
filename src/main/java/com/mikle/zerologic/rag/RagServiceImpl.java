package com.mikle.zerologic.rag;

import cn.hutool.json.JSONUtil;
import com.mikle.zerologic.service.RagRetrievalLogService;
import com.mikle.zerologic.model.entity.RagRetrievalLog;
import com.mikle.zerologic.rag.model.RagResult;
import com.mikle.zerologic.rag.model.RagRetrievedChunk;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RagServiceImpl implements RagService {

    @Value("${rag.top-k:5}")
    private int topK;

    @Value("${rag.max-injected-chars:6000}")
    private int maxInjectedChars;

    @Resource
    private VectorSearchService vectorSearchService;

    @Resource
    private RagRetrievalLogService ragRetrievalLogService;

    @Override
    public RagResult retrieve(Long taskId, Long appId, Long userId, Long attachmentId, String query) {
        List<RagRetrievedChunk> retrievedChunks = vectorSearchService.search(
                appId, userId, attachmentId, query, topK);

        StringBuilder contextBuilder = new StringBuilder();
        List<RagRetrievedChunk> injectedReferences = new ArrayList<>();
        for (RagRetrievedChunk chunk : retrievedChunks) {
            String block = """
                    [%d] 来源：%s，片段 %d，相似度 %.2f
                    %s

                    """.formatted(
                    injectedReferences.size() + 1,
                    chunk.getDocumentName(),
                    chunk.getChunkIndex(),
                    chunk.getScore(),
                    chunk.getContent()
            );
            int remaining = maxInjectedChars - contextBuilder.length();
            if (remaining <= 0) {
                break;
            }
            contextBuilder.append(block, 0, Math.min(block.length(), remaining));
            injectedReferences.add(chunk);
        }

        RagResult result = RagResult.builder()
                .contextText(contextBuilder.toString())
                .references(List.copyOf(injectedReferences))
                .build();
        saveRetrievalLog(taskId, appId, userId, query, result);
        return result;
    }

    private void saveRetrievalLog(Long taskId, Long appId, Long userId, String query, RagResult result) {
        try {
            RagRetrievalLog retrievalLog = RagRetrievalLog.builder()
                    .taskId(taskId)
                    .appId(appId)
                    .userId(userId)
                    .queryText(query == null ? "" : query)
                    .topK(topK)
                    .hitCount(result.getReferences().size())
                    .injectedCharLength(result.getContextText().length())
                    .hitChunksJson(JSONUtil.toJsonStr(result.getReferences()))
                    .build();
            if (!ragRetrievalLogService.save(retrievalLog)) {
                log.warn("RAG 检索日志保存失败，taskId={}", taskId);
            }
        } catch (Exception e) {
            // 检索日志属于观测数据，不能因为日志写入失败阻断代码生成。
            log.warn("RAG 检索日志写入异常，taskId={}", taskId, e);
        }
    }
}
