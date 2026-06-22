package com.mikle.zerologic.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mikle.zerologic.model.entity.RagRetrievalLog;
import com.mikle.zerologic.model.vo.RagReferenceVO;
import com.mikle.zerologic.model.vo.RagRetrievalVO;
import com.mikle.zerologic.rag.model.RagRetrievedChunk;
import com.mikle.zerologic.service.RagRetrievalLogQueryService;
import com.mikle.zerologic.service.RagRetrievalLogService;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class RagRetrievalLogQueryServiceImpl implements RagRetrievalLogQueryService {

    private static final int MAX_SNIPPET_LENGTH = 400;

    @Resource
    private RagRetrievalLogService ragRetrievalLogService;

    @Override
    public RagRetrievalVO getByTaskId(Long taskId, Long appId, Long userId) {
        if (taskId == null || appId == null || userId == null) {
            return null;
        }
        QueryWrapper query = baseQuery(appId, userId)
                .eq("taskId", taskId)
                .orderBy("createTime", false)
                .limit(1);
        RagRetrievalLog retrievalLog = ragRetrievalLogService.getOne(query);
        return retrievalLog == null ? null : toVO(retrievalLog);
    }

    @Override
    public Map<Long, RagRetrievalVO> listByTaskIds(Collection<Long> taskIds,
                                                   Long appId, Long userId) {
        if (taskIds == null || taskIds.isEmpty() || appId == null || userId == null) {
            return Map.of();
        }
        List<Long> validTaskIds = taskIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (validTaskIds.isEmpty()) {
            return Map.of();
        }

        QueryWrapper query = baseQuery(appId, userId)
                .in("taskId", validTaskIds)
                .orderBy("createTime", false);
        Map<Long, RagRetrievalVO> result = new LinkedHashMap<>();
        for (RagRetrievalLog retrievalLog : ragRetrievalLogService.list(query)) {
            result.putIfAbsent(retrievalLog.getTaskId(), toVO(retrievalLog));
        }
        return Collections.unmodifiableMap(result);
    }

    private QueryWrapper baseQuery(Long appId, Long userId) {
        return QueryWrapper.create()
                .eq("appId", appId)
                .eq("userId", userId);
    }

    private RagRetrievalVO toVO(RagRetrievalLog retrievalLog) {
        RagRetrievalVO vo = new RagRetrievalVO();
        vo.setTaskId(retrievalLog.getTaskId());
        vo.setQueryText(retrievalLog.getQueryText());
        vo.setTopK(retrievalLog.getTopK());
        vo.setHitCount(retrievalLog.getHitCount());
        vo.setInjectedCharLength(retrievalLog.getInjectedCharLength());
        vo.setCreateTime(retrievalLog.getCreateTime());
        vo.setReferences(parseReferences(retrievalLog));
        return vo;
    }

    private List<RagReferenceVO> parseReferences(RagRetrievalLog retrievalLog) {
        if (StrUtil.isBlank(retrievalLog.getHitChunksJson())) {
            return List.of();
        }
        try {
            return JSONUtil.toList(
                            JSONUtil.parseArray(retrievalLog.getHitChunksJson()),
                            RagRetrievedChunk.class)
                    .stream()
                    .map(this::toReferenceVO)
                    .toList();
        } catch (RuntimeException e) {
            log.warn("RAG 命中记录解析失败，retrievalLogId={}, taskId={}",
                    retrievalLog.getId(), retrievalLog.getTaskId(), e);
            return List.of();
        }
    }

    private RagReferenceVO toReferenceVO(RagRetrievedChunk chunk) {
        RagReferenceVO vo = new RagReferenceVO();
        vo.setDocumentId(chunk.getDocumentId());
        vo.setChunkId(chunk.getChunkId());
        vo.setDocumentName(chunk.getDocumentName());
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setContentSnippet(StrUtil.subPre(StrUtil.nullToEmpty(chunk.getContent()), MAX_SNIPPET_LENGTH));
        vo.setScore(chunk.getScore());
        return vo;
    }
}
