package com.mikle.zerologic.rag;

import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.rag.model.DocumentChunk;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentChunkerTest {

    @Test
    void shouldSplitContentWithOverlap() {
        DocumentChunker chunker = createChunker(5, 2);

        List<DocumentChunk> chunks = chunker.split("abcdefghij");

        assertEquals(3, chunks.size());
        assertEquals("abcde", chunks.get(0).getContent());
        assertEquals("defgh", chunks.get(1).getContent());
        assertEquals("ghij", chunks.get(2).getContent());
    }

    @Test
    void shouldRejectOverlapNotSmallerThanChunkSize() {
        DocumentChunker chunker = createChunker(5, 5);

        assertThrows(BusinessException.class, () -> chunker.split("abcdefghij"));
    }

    private DocumentChunker createChunker(int chunkSize, int chunkOverlap) {
        DocumentChunker chunker = new DocumentChunker();
        ReflectionTestUtils.setField(chunker, "chunkSize", chunkSize);
        ReflectionTestUtils.setField(chunker, "chunkOverlap", chunkOverlap);
        return chunker;
    }
}
