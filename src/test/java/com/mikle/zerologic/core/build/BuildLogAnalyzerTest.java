package com.mikle.zerologic.core.build;

import com.mikle.zerologic.config.RepairProperties;
import com.mikle.zerologic.core.build.model.BuildDiagnosis;
import com.mikle.zerologic.core.build.model.BuildResult;
import com.mikle.zerologic.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class BuildLogAnalyzerTest {
    private BuildLogAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new BuildLogAnalyzer();
        RepairProperties properties = new RepairProperties();
        ReflectionTestUtils.setField(analyzer, "repairProperties", properties);
    }

    @Test
    void extractsVueSourceFilesFromBuildFailure() {
        BuildResult result = BuildResult.builder().success(false).timedOut(false)
                .command("npm.cmd ci && npm.cmd run build")
                .logText("src/views/Home.vue:12:4 - error TS2322: bad type\nError: build failed")
                .build();

        BuildDiagnosis diagnosis = analyzer.analyze(CodeGenTypeEnum.VUE_PROJECT, result);

        assertTrue(diagnosis.isRepairable());
        assertEquals(java.util.List.of("src/views/Home.vue"), diagnosis.getSuspectedFiles());
        assertTrue(diagnosis.getSummary().contains("TS2322"));
    }

    @Test
    void doesNotRepairTimeoutOrInstallFailure() {
        BuildResult timeout = BuildResult.builder().success(false).timedOut(true)
                .command("npm.cmd run build").build();
        BuildResult installFailure = BuildResult.builder().success(false).timedOut(false)
                .command("npm.cmd ci").build();

        assertFalse(analyzer.analyze(CodeGenTypeEnum.VUE_PROJECT, timeout).isRepairable());
        assertFalse(analyzer.analyze(CodeGenTypeEnum.VUE_PROJECT, installFailure).isRepairable());
        assertFalse(analyzer.analyze(CodeGenTypeEnum.HTML, installFailure).isRepairable());
    }
}
