package com.mikle.zerologic.rag;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.rag.model.DocumentChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 把文档正文切成 chunk
 */
@Component
public class DocumentChunker {

    @Value("${rag.chunk-size:1000}")
    private int chunkSize;

    @Value("${rag.chunk-overlap:120}")
    private int chunkOverlap;

    public List<DocumentChunk> split(String content) {
        if (StrUtil.isBlank(content)) {
            return List.of();
        }
        if (chunkSize <= 0 || chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "RAG 切片配置错误：chunk-size 必须大于 chunk-overlap 且二者不能为负数");
        }

        String normalized = content.replace("\r\n", "\n").trim();
        List<DocumentChunk> chunks = new ArrayList<>();

        int start = 0;
        int index = 0;

        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            String chunkContent = normalized.substring(start, end).trim();

            if (StrUtil.isNotBlank(chunkContent)) {
                chunks.add(DocumentChunk.builder()
                        .chunkIndex(index++)
                        .content(chunkContent)
                        .contentHash(SecureUtil.sha256(chunkContent))
                        .charLength(chunkContent.length())
                        .build());
            }

            if (end >= normalized.length()) {
                break;
            }

            start = Math.max(0, end - chunkOverlap);
        }

        return chunks;
    }
}
