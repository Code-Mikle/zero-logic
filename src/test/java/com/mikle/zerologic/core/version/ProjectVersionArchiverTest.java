package com.mikle.zerologic.core.version;

import com.mikle.zerologic.core.build.model.BuildResult;
import com.mikle.zerologic.model.entity.ProjectVersion;
import com.mikle.zerologic.model.enums.CodeGenTypeEnum;
import com.mikle.zerologic.service.ProjectVersionService;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectVersionArchiverTest {

    @Mock
    private ProjectVersionService projectVersionService;

    @TempDir
    Path tempDir;

    @Test
    void archivesSourceAndArtifactForHtmlProject() throws Exception {
        Path projectDir = tempDir.resolve("html_42");
        Files.createDirectories(projectDir.resolve("node_modules/pkg"));
        Files.createDirectories(projectDir.resolve("dist"));
        Files.writeString(projectDir.resolve("index.html"), "<html></html>");
        Files.writeString(projectDir.resolve("node_modules/pkg/index.js"), "ignored");
        Files.writeString(projectDir.resolve("dist/index.html"), "ignored");

        ProjectVersionPathResolver resolver = new ProjectVersionPathResolver() {
            @Override
            public Path resolveVersionDir(Long appId, Integer versionNo) {
                return tempDir.resolve("versions").resolve("app_" + appId).resolve("v" + versionNo);
            }
        };
        ProjectVersionArchiver archiver = new ProjectVersionArchiver();
        ReflectionTestUtils.setField(archiver, "projectVersionService", projectVersionService);
        ReflectionTestUtils.setField(archiver, "pathResolver", resolver);
        when(projectVersionService.getOne(any(QueryWrapper.class))).thenReturn(null);
        when(projectVersionService.createBuiltVersion(anyLong(), anyLong(), anyLong(), eq(1),
                anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(ProjectVersion.builder().id(100L).versionNo(1).build());

        archiver.archiveBuiltVersion(42L, 7L, 12L, CodeGenTypeEnum.HTML, projectDir,
                BuildResult.builder()
                        .success(true)
                        .artifactPath(projectDir.toString())
                        .buildRecordId(99L)
                        .build());

        Path versionDir = tempDir.resolve("versions").resolve("app_42").resolve("v1");
        assertTrue(Files.isRegularFile(versionDir.resolve("source/index.html")));
        assertTrue(Files.isRegularFile(versionDir.resolve("artifact/index.html")));
        assertFalse(Files.exists(versionDir.resolve("source/node_modules")));
        assertFalse(Files.exists(versionDir.resolve("source/dist")));
    }
}
