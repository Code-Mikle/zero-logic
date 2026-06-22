package com.mikle.zerologic.rag;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DashScopeEmbeddingServiceImpl implements EmbeddingService {

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${rag.embedding.model-name}")
    private String modelName;

    @Value("${rag.embedding.dimension:1024}")
    private int dimension;

    private final TextEmbedding textEmbedding = new TextEmbedding();

    @Override
    public List<Double> embed(String text) {

        if (text == null || text.isBlank()) {
            return List.of();
        }

        try {
            TextEmbeddingParam param = TextEmbeddingParam
                    .builder()
                    .apiKey(apiKey)
                    .model(modelName)
                    .texts(List.of(text))
                    .parameter("dimension", dimension)  // 指定向量维度（仅 text-embedding-v3及 text-embedding-v4支持该参数）
                    .build();

            TextEmbeddingResult result = textEmbedding.call(param);

            if (result == null || result.getOutput() == null
                    || result.getOutput().getEmbeddings() == null
                    || result.getOutput().getEmbeddings().isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Embedding 服务未返回向量");
            }
            List<Double> embedding = result.getOutput().getEmbeddings().getFirst().getEmbedding();
            if (embedding == null || embedding.size() != dimension) {
                int actualDimension = embedding == null ? 0 : embedding.size();
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        "Embedding 向量维度异常，期望 " + dimension + "，实际 " + actualDimension);
            }
            return embedding;

        } catch (ApiException | NoApiKeyException e) {
            log.error("DashScope Embedding 调用失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Embedding 服务调用失败");
        }
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public int getDimension() {
        return dimension;
    }
}
