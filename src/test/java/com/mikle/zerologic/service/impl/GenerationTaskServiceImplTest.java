package com.mikle.zerologic.service.impl;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GenerationTaskServiceImplTest {

    @Test
    void doesNotStartTaskBeforeStreamSubscription() {
        GenerationTaskServiceImpl service = new GenerationTaskServiceImpl();

        Flux<String> stream = assertDoesNotThrow(() -> service.streamGenerateTask(12L, null));

        assertThrows(RuntimeException.class, stream::blockLast);
    }
}
