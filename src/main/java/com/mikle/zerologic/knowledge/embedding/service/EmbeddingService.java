package com.mikle.zerologic.knowledge.embedding.service;

import java.util.List;

/**
 * 文本转向量
 */
public interface EmbeddingService {

    List<Double> embed(String text);

    String getModelName();

    int getDimension();
}
