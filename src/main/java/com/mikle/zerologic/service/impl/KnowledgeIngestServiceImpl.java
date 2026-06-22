package com.mikle.zerologic.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.exception.ThrowUtils;
import com.mikle.zerologic.service.KnowledgeChunkService;
import com.mikle.zerologic.service.KnowledgeDocumentService;
import com.mikle.zerologic.service.KnowledgeEmbeddingService;
import com.mikle.zerologic.model.entity.*;
import com.mikle.zerologic.model.enums.KnowledgeDocumentStatusEnum;
import com.mikle.zerologic.model.enums.KnowledgeDocumentTypeEnum;
import com.mikle.zerologic.rag.DocumentChunker;
import com.mikle.zerologic.rag.EmbeddingJsonUtils;
import com.mikle.zerologic.rag.EmbeddingService;
import com.mikle.zerologic.rag.model.DocumentChunk;
import com.mikle.zerologic.service.KnowledgeIngestService;
import com.mikle.zerologic.service.PromptAttachmentService;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class KnowledgeIngestServiceImpl implements KnowledgeIngestService {

    @Resource
    private PromptAttachmentService promptAttachmentService;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Resource
    private DocumentChunker documentChunker;

    @Resource
    private KnowledgeChunkService knowledgeChunkService;

    @Resource
    private EmbeddingService embeddingService;

    @Resource
    private KnowledgeEmbeddingService knowledgeEmbeddingService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long ingestAttachment(Long attachmentId, Long appId, User loginUser) {

        ThrowUtils.throwIf(attachmentId == null || appId == null || loginUser == null
                        || loginUser.getId() == null,
                ErrorCode.PARAMS_ERROR, "存在参数为空");

        QueryWrapper attachmentQuery = QueryWrapper.create()
                .eq("id", attachmentId)
                .eq("userId", loginUser.getId())
                .eq("appId", appId);

        PromptAttachment attachment = promptAttachmentService.getOne(attachmentQuery);
        ThrowUtils.throwIf(attachment == null, ErrorCode.NO_AUTH_ERROR,
                "该附件不属于当前用户和APP或该附件不存在");
        ThrowUtils.throwIf(attachment.getContent() == null || attachment.getContent().isBlank(),
                ErrorCode.PARAMS_ERROR, "附件内容为空，无法导入知识库");

        String sha256 = SecureUtil.sha256(attachment.getContent());

        QueryWrapper knowledgeDocumentQuery = QueryWrapper.create()
                .eq("appId", appId)
                .eq("attachmentId", attachmentId);

        KnowledgeDocument knowledgeDocument = knowledgeDocumentService.getOne(knowledgeDocumentQuery);
        if (knowledgeDocument != null) {
            return knowledgeDocument.getId();
        }

        // insert into `knowledge_document`
        KnowledgeDocument documentToInsert = KnowledgeDocument.builder()
                .appId(appId)
                .userId(loginUser.getId())
                .attachmentId(attachmentId)
                .documentName(attachment.getFileName())
                .documentType(KnowledgeDocumentTypeEnum.ATTACHMENT.getValue())
                .sourceType(KnowledgeDocumentTypeEnum.ATTACHMENT.getValue())
                .contentHash(sha256)
                .status(KnowledgeDocumentStatusEnum.ACTIVE.getValue())
                .build();
        boolean saved = knowledgeDocumentService.save(documentToInsert);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR,
                "ingestAttachment 插入 knowledge_document 表失败");

        // insert into `knowledge_chunk`
        List<DocumentChunk> documentChunkList = documentChunker.split(attachment.getContent());
        ThrowUtils.throwIf(documentChunkList.isEmpty(), ErrorCode.PARAMS_ERROR,
                "附件内容无法生成有效切片");
        List<KnowledgeChunk> knowledgeChunkList = documentChunkList.stream()
                .map(item -> KnowledgeChunk.builder()
                            .documentId(documentToInsert.getId())
                            .appId(appId)
                            .userId(loginUser.getId())
                            .chunkIndex(item.getChunkIndex())
                            .content(item.getContent())
                            .contentHash(item.getContentHash())
                            .charLength(item.getCharLength())
                            .build())
                .toList();
        // 后续写入向量时需要 chunk 主键。逐条保存可确保不同 JDBC 驱动下都能稳定回填自增 ID。
        for (KnowledgeChunk knowledgeChunk : knowledgeChunkList) {
            boolean chunkSaved = knowledgeChunkService.save(knowledgeChunk);
            ThrowUtils.throwIf(!chunkSaved || knowledgeChunk.getId() == null,
                    ErrorCode.OPERATION_ERROR, "ingestAttachment 插入 knowledge_chunk 表失败");
        }

        // insert into `knowledge_embedding`
        List<KnowledgeEmbedding> knowledgeEmbeddingList = new ArrayList<>(knowledgeChunkList.size());
        for (KnowledgeChunk item : knowledgeChunkList) {
            List<Double> embedded = embeddingService.embed(item.getContent());
            ThrowUtils.throwIf(embedded.isEmpty(), ErrorCode.OPERATION_ERROR,
                    "Embedding 服务返回了空向量");
            knowledgeEmbeddingList.add(KnowledgeEmbedding.builder()
                    .chunkId(item.getId())
                    .appId(item.getAppId())
                    .userId(item.getUserId())
                    .embeddingModel(embeddingService.getModelName())
                    .embeddingDimension(embedded.size())
                    .embeddingJson(EmbeddingJsonUtils.toJson(embedded))
                    .build());
        }
        boolean embeddingSaved = knowledgeEmbeddingService.saveBatch(knowledgeEmbeddingList);
        ThrowUtils.throwIf(!embeddingSaved, ErrorCode.OPERATION_ERROR,
                "ingestAttachment 插入 knowledge_embedding 表失败");

        return documentToInsert.getId();
    }
}
