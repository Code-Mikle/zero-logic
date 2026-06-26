package com.mikle.zerologic.ai.tools.policy;

import cn.hutool.json.JSONObject;
import com.mikle.zerologic.ai.tools.ProjectToolPathResolver;
import com.mikle.zerologic.config.ToolPolicyProperties;
import com.mikle.zerologic.model.enums.ToolRiskLevelEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolPolicyServiceImplTest {

    private ToolPolicyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ToolPolicyServiceImpl();
        ReflectionTestUtils.setField(service, "properties", new ToolPolicyProperties());
        ReflectionTestUtils.setField(service, "pathResolver", new ProjectToolPathResolver());
    }

    @Test
    void allowsReadOperation() {
        ToolPolicyResult result = service.check(request(ToolOperationEnum.READ,
                "repair", new JSONObject().set("relativeFilePath", ".env")));

        assertTrue(result.isAllowed());
    }

    @Test
    void rejectsProtectedFileModification() {
        ToolPolicyResult result = service.check(request(ToolOperationEnum.MODIFY,
                "generate", new JSONObject()
                        .set("relativeFilePath", "package.json")
                        .set("newContent", "{}")));

        assertFalse(result.isAllowed());
    }

    @Test
    void allowsCreatingProtectedScaffoldFileDuringGeneration() {
        ToolPolicyResult result = service.check(ToolPolicyRequest.builder()
                .toolName("test")
                .operation(ToolOperationEnum.WRITE)
                .riskLevel(ToolRiskLevelEnum.MEDIUM)
                .appId(987654321L)
                .taskId(12L)
                .userId(7L)
                .callSource("generate")
                .arguments(new JSONObject()
                        .set("relativeFilePath", "package.json")
                        .set("content", "{}"))
                .build());

        assertTrue(result.isAllowed());
    }

    @Test
    void rejectsDeleteDuringRepair() {
        ToolPolicyResult result = service.check(request(ToolOperationEnum.DELETE,
                "repair", new JSONObject().set("relativeFilePath", "src/Unused.vue")));

        assertFalse(result.isAllowed());
    }

    @Test
    void rejectsProtectedDirectoryWrite() {
        ToolPolicyResult result = service.check(request(ToolOperationEnum.WRITE,
                "generate", new JSONObject()
                        .set("relativeFilePath", "dist/index.html")
                        .set("content", "<html></html>")));

        assertFalse(result.isAllowed());
    }

    @Test
    void rejectsOversizedWrite() {
        ToolPolicyResult result = service.check(request(ToolOperationEnum.WRITE,
                "generate", new JSONObject()
                        .set("relativeFilePath", "src/App.vue")
                        .set("content", "a".repeat(300_001))));

        assertFalse(result.isAllowed());
    }

    private ToolPolicyRequest request(ToolOperationEnum operation, String callSource, JSONObject arguments) {
        return ToolPolicyRequest.builder()
                .toolName("test")
                .operation(operation)
                .riskLevel(ToolRiskLevelEnum.MEDIUM)
                .appId(42L)
                .taskId(12L)
                .userId(7L)
                .callSource(callSource)
                .arguments(arguments)
                .build();
    }
}
