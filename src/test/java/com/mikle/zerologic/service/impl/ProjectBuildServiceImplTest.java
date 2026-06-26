package com.mikle.zerologic.service.impl;

import com.mikle.zerologic.config.BuildProperties;
import com.mikle.zerologic.core.build.model.BuildResult;
import com.mikle.zerologic.core.builder.VueProjectBuilder;
import com.mikle.zerologic.model.entity.GenerationBuildRecord;
import com.mikle.zerologic.model.enums.CodeGenTypeEnum;
import com.mikle.zerologic.service.GenerationBuildRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectBuildServiceImplTest {

    @Mock
    private GenerationBuildRecordService generationBuildRecordService;

    @Mock
    private VueProjectBuilder vueProjectBuilder;

    @Mock
    private BuildProperties buildProperties;

    @InjectMocks
    private ProjectBuildServiceImpl projectBuildService;

    @TempDir
    Path projectPath;

    @BeforeEach
    void setUp() {
        when(buildProperties.isEnabled()).thenReturn(true);
        when(generationBuildRecordService.createRunning(
                anyLong(), anyLong(), anyLong(), anyInt(), anyString(), anyString()))
                .thenReturn(GenerationBuildRecord.builder().id(99L).build());
    }

    @Test
    void shouldPassWhenHtmlEntryExists() throws Exception {
        Files.writeString(projectPath.resolve("index.html"), "<html></html>");

        BuildResult result = projectBuildService.build(
                1L, 2L, 3L, CodeGenTypeEnum.HTML, projectPath, 1);

        assertTrue(result.getSuccess());
        verify(generationBuildRecordService).finish(anyLong(), any(BuildResult.class));
    }

    @Test
    void shouldFailWhenHtmlEntryDoesNotExist() {
        BuildResult result = projectBuildService.build(
                1L, 2L, 3L, CodeGenTypeEnum.HTML, projectPath, 1);

        assertFalse(result.getSuccess());
        verify(generationBuildRecordService).finish(anyLong(), any(BuildResult.class));
    }
}
