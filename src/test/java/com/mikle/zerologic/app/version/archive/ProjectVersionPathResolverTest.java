package com.mikle.zerologic.app.version.archive;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectVersionPathResolverTest {

    private final ProjectVersionPathResolver resolver = new ProjectVersionPathResolver();

    @Test
    void resolvesVersionDirectories() {
        Path sourceDir = resolver.resolveSourceDir(42L, 3);
        Path artifactDir = resolver.resolveArtifactDir(42L, 3);

        assertTrue(sourceDir.endsWith(Path.of("project_versions", "app_42", "v3", "source")));
        assertTrue(artifactDir.endsWith(Path.of("project_versions", "app_42", "v3", "artifact")));
    }
}
