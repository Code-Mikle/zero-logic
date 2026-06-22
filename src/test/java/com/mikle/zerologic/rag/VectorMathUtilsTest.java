package com.mikle.zerologic.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VectorMathUtilsTest {

    @Test
    void shouldCalculateCosineSimilarity() {
        assertEquals(1D, VectorMathUtils.cosineSimilarity(
                List.of(1D, 2D), List.of(1D, 2D)), 0.000001D);
        assertEquals(0D, VectorMathUtils.cosineSimilarity(
                List.of(1D, 0D), List.of(0D, 1D)), 0.000001D);
    }

    @Test
    void shouldReturnZeroForDifferentDimensions() {
        assertEquals(0D, VectorMathUtils.cosineSimilarity(
                List.of(1D), List.of(1D, 2D)), 0.000001D);
    }
}
