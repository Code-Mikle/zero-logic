package com.mikle.zerologic.ai.tools;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProjectToolPathResolverTest {
    private final ProjectToolPathResolver resolver = new ProjectToolPathResolver();

    @Test
    void resolvesPathInsideGeneratedProject() throws Exception {
        Path result = resolver.resolve(42L, "src/App.vue");
        System.out.println(result.toString());
        assertTrue(result.endsWith(Path.of("vue_project_42", "src", "App.vue")));
    }

    @Test
    void rejectsTraversalAndAbsolutePath() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(42L, "../secret.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve(42L, Path.of("C:/Windows/win.ini").toString()));
    }
}
