package com.mikle.zerologic.rag;

import java.util.List;

/**
 * 计算余弦相似度
 */
public final class VectorMathUtils {

    private VectorMathUtils() {
    }

    public static double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.size() != b.size() || a.isEmpty()) {
            return 0D;
        }

        double dot = 0D;
        double normA = 0D;
        double normB = 0D;

        for (int i = 0; i < a.size(); i++) {
            double x = a.get(i);
            double y = b.get(i);
            dot += x * y;
            normA += x * x;
            normB += y * y;
        }

        if (normA == 0D || normB == 0D) {
            return 0D;
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
