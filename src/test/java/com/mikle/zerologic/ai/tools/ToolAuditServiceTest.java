package com.mikle.zerologic.ai.tools;

import cn.hutool.json.JSONObject;
import com.mikle.zerologic.model.entity.ToolCallRecord;
import com.mikle.zerologic.model.enums.ToolRiskLevelEnum;
import com.mikle.zerologic.service.ToolCallRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolAuditServiceTest {

    @Mock
    private ToolCallRecordService toolCallRecordService;

    @AfterEach
    void tearDown() {
        ToolExecutionContextHolder.clear(42L);
    }

    @Test
    void recordsSuccessfulToolCallWithContext() {
        ToolAuditService auditService = createAuditService();
        when(toolCallRecordService.save(any(ToolCallRecord.class))).thenReturn(true);
        ToolExecutionContextHolder.set(ToolExecutionContext.builder()
                .taskId(12L)
                .appId(42L)
                .userId(7L)
                .callSource("generate")
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

        auditService.audit(new TestTool(), 42L, new JSONObject(),
                () -> "错误：不允许删除重要文件 - package.json");

        ArgumentCaptor<ToolCallRecord> captor = ArgumentCaptor.forClass(ToolCallRecord.class);
        verify(toolCallRecordService).save(captor.capture());
        assertEquals("rejected", captor.getValue().getStatus());
    }

    private ToolAuditService createAuditService() {
        ToolAuditService auditService = new ToolAuditService();
        ReflectionTestUtils.setField(auditService, "toolCallRecordService", toolCallRecordService);
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
