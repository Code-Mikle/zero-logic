package com.mikle.zerologic.generation.tool.execution;

import cn.hutool.json.JSONObject;
import com.mikle.zerologic.generation.tool.policy.ToolPolicyResult;
import com.mikle.zerologic.generation.tool.policy.ToolPolicyService;
import com.mikle.zerologic.generation.tool.execution.ToolAuditService;
import com.mikle.zerologic.generation.tool.execution.ToolExecutionContext;
import com.mikle.zerologic.generation.tool.execution.ToolExecutionContextHolder;
import com.mikle.zerologic.generation.tool.file.BaseTool;
import com.mikle.zerologic.generation.tool.model.entity.ToolCallRecord;
import com.mikle.zerologic.generation.tool.model.enums.ToolRiskLevelEnum;
import com.mikle.zerologic.generation.tool.model.enums.ToolCallSourceEnum;
import com.mikle.zerologic.generation.tool.service.ToolCallRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolAuditServiceTest {

    @Mock
    private ToolCallRecordService toolCallRecordService;

    @Mock
    private ToolPolicyService toolPolicyService;

    @AfterEach
    void tearDown() {
        ToolExecutionContextHolder.clear(42L);
    }

    @Test
    void recordsSuccessfulToolCallWithContext() {
        ToolAuditService auditService = createAuditService();
        when(toolCallRecordService.save(any(ToolCallRecord.class))).thenReturn(true);
        when(toolPolicyService.check(any())).thenReturn(ToolPolicyResult.allow());
        ToolExecutionContextHolder.set(ToolExecutionContext.builder()
                .taskId(12L)
                .appId(42L)
                .userId(7L)
                .callSource(ToolCallSourceEnum.GENERATE)
                .build());

        String result = auditService.audit(new TestTool(), 42L,
                new JSONObject().set("relativeFilePath", "src/App.vue"),
                () -> "文件写入成功: src/App.vue");

        assertEquals("文件写入成功: src/App.vue", result);
        ArgumentCaptor<ToolCallRecord> captor = ArgumentCaptor.forClass(ToolCallRecord.class);
        verify(toolCallRecordService).save(captor.capture());
        ToolCallRecord record = captor.getValue();
        assertEquals(12L, record.getTaskId());
        assertEquals(42L, record.getAppId());
        assertEquals(7L, record.getUserId());
        assertEquals("generate", record.getCallSource());
        assertEquals("success", record.getStatus());
        assertEquals("testTool", record.getToolName());
        assertEquals("medium", record.getRiskLevel());
    }

    @Test
    void marksRejectedResult() {
        ToolAuditService auditService = createAuditService();
        when(toolCallRecordService.save(any(ToolCallRecord.class))).thenReturn(true);
        when(toolPolicyService.check(any())).thenReturn(ToolPolicyResult.reject("受保护路径不允许执行该工具操作"));

        String result = auditService.audit(new TestTool(), 42L, new JSONObject(),
                () -> "不会执行");

        assertEquals("工具调用被安全策略拒绝：受保护路径不允许执行该工具操作", result);
        ArgumentCaptor<ToolCallRecord> captor = ArgumentCaptor.forClass(ToolCallRecord.class);
        verify(toolCallRecordService).save(captor.capture());
        assertEquals("rejected", captor.getValue().getStatus());
        assertEquals("受保护路径不允许执行该工具操作", captor.getValue().getErrorMessage());
    }

    @Test
    void exposesToolContextAcrossThreads() throws Exception {
        ToolExecutionContext context = ToolExecutionContext.builder()
                .taskId(12L)
                .appId(42L)
                .userId(7L)
                .callSource(ToolCallSourceEnum.GENERATE)
                .build();
        ToolExecutionContextHolder.set(context);

        ToolExecutionContext contextFromCallbackThread = CompletableFuture
                .supplyAsync(() -> ToolExecutionContextHolder.get(42L))
                .get(3, TimeUnit.SECONDS);

        assertSame(context, contextFromCallbackThread);
    }

    private ToolAuditService createAuditService() {
        ToolAuditService auditService = new ToolAuditService();
        ReflectionTestUtils.setField(auditService, "toolCallRecordService", toolCallRecordService);
        ReflectionTestUtils.setField(auditService, "toolPolicyService", toolPolicyService);
        return auditService;
    }

    private static class TestTool extends BaseTool {

        @Override
        public String getToolName() {
            return "testTool";
        }

        @Override
        public String getDisplayName() {
            return "测试工具";
        }

        @Override
        public ToolRiskLevelEnum getRiskLevel() {
            return ToolRiskLevelEnum.MEDIUM;
        }

        @Override
        public String generateToolExecutedResult(JSONObject arguments) {
            return "";
        }
    }
}
