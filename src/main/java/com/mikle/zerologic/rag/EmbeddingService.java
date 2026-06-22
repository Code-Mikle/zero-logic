package com.mikle.zerologic.rag;

import java.util.List;

/**
 * 文本转向量
 */
public interface EmbeddingService {

    List<Double> embed(String text);

    String getModelName();

    int getDimension();
}
