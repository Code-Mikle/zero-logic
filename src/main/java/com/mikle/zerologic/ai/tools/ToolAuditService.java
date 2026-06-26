package com.mikle.zerologic.ai.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.mikle.zerologic.ai.tools.policy.ToolPolicyRequest;
import com.mikle.zerologic.ai.tools.policy.ToolPolicyResult;
import com.mikle.zerologic.ai.tools.policy.ToolPolicyService;
import com.mikle.zerologic.model.entity.ToolCallRecord;
import com.mikle.zerologic.model.enums.ToolCallStatusEnum;
import com.mikle.zerologic.service.ToolCallRecordService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
public class ToolAuditService {

    private static final int MAX_ARGUMENTS_LENGTH = 12000;

    private static final int MAX_RESULT_LENGTH = 12000;

    private static final int MAX_ERROR_LENGTH = 2048;

    private static final int MAX_ARGUMENT_VALUE_LENGTH = 2000;

    @Resource
    private ToolCallRecordService toolCallRecordService;

    @Resource
    private ToolPolicyService toolPolicyService;

    public String audit(BaseTool tool, Long appId, JSONObject arguments, Supplier<String> action) {
        long startTime = System.currentTimeMillis();
        ToolExecutionContext context = ToolExecutionContextHolder.get(appId);
        String result = null;
        String status = ToolCallStatusEnum.SUCCESS.getValue();
        String errorMessage = null;
        try {
            ToolPolicyResult policyResult = toolPolicyService.check(buildPolicyRequest(tool, appId, context, arguments));
            if (!policyResult.isAllowed()) {
                status = ToolCallStatusEnum.REJECTED.getValue();
                errorMessage = policyResult.getReason();
                result = "工具调用被安全策略拒绝：" + policyResult.getReason();
                return result;
            }
            result = action.get();
            status = inferStatus(result);
            return result;
        } catch (RuntimeException e) {
            status = ToolCallStatusEnum.FAILED.getValue();
            errorMessage = StrUtil.blankToDefault(e.getMessage(), e.getClass().getSimpleName());
            throw e;
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            saveRecord(tool, appId, context, arguments, result, status, errorMessage, durationMs);
        }
    }

    private ToolPolicyRequest buildPolicyRequest(BaseTool tool, Long appId,
                                                 ToolExecutionContext context, JSONObject arguments) {
        return ToolPolicyRequest.builder()
                .toolName(tool.getToolName())
                .operation(tool.getOperation())
                .riskLevel(tool.getRiskLevel())
                .appId(resolveAppId(appId, context))
                .taskId(context == null ? null : context.getTaskId())
                .userId(context == null ? null : context.getUserId())
                .callSource(context == null ? null : context.getCallSource())
                .arguments(arguments)
                .build();
    }

    private void saveRecord(BaseTool tool, Long appId, ToolExecutionContext context, JSONObject arguments,
                            String result, String status, String errorMessage, long durationMs) {
        try {
            ToolCallRecord record = ToolCallRecord.builder()
                    .taskId(context == null ? null : context.getTaskId())
                    .appId(resolveAppId(appId, context))
                    .userId(context == null ? null : context.getUserId())
                    .toolName(tool.getToolName())
                    .displayName(tool.getDisplayName())
                    .toolCategory(tool.getCategory().getValue())
                    .riskLevel(tool.getRiskLevel().getValue())
                    .callSource(context == null ? null : context.getCallSource())
                    .status(status)
                    .argumentsJson(StrUtil.subPre(JSONUtil.toJsonStr(sanitizeArguments(arguments)), MAX_ARGUMENTS_LENGTH))
                    .resultSummary(StrUtil.subPre(result, MAX_RESULT_LENGTH))
                    .errorMessage(StrUtil.subPre(errorMessage, MAX_ERROR_LENGTH))
                    .durationMs(durationMs)
                    .build();
            if (!toolCallRecordService.save(record)) {
                log.warn("工具调用审计记录保存失败，toolName={}, appId={}", tool.getToolName(), appId);
            }
        } catch (Exception e) {
            log.warn("工具调用审计记录写入异常，toolName={}, appId={}", tool.getToolName(), appId, e);
        }
    }

    private Long resolveAppId(Long appId, ToolExecutionContext context) {
        if (appId != null) {
            return appId;
        }
        return context == null ? 0L : context.getAppId();
    }

    private String inferStatus(String result) {
        if (StrUtil.isBlank(result)) {
            return ToolCallStatusEnum.SUCCESS.getValue();
        }
        String normalized = result.trim().toLowerCase();
        if (normalized.contains("不允许")
                || normalized.contains("not allowed")
                || normalized.contains("forbidden")) {
            return ToolCallStatusEnum.REJECTED.getValue();
        }
        if (normalized.startsWith("错误")
                || normalized.contains("失败")
                || normalized.startsWith("error")) {
            return ToolCallStatusEnum.FAILED.getValue();
        }
        return ToolCallStatusEnum.SUCCESS.getValue();
    }

    private JSONObject sanitizeArguments(JSONObject arguments) {
        JSONObject sanitized = new JSONObject();
        if (arguments == null) {
            return sanitized;
        }
        for (String key : arguments.keySet()) {
            Object value = arguments.get(key);
            if (value == null) {
                sanitized.set(key, null);
                continue;
            }
            String lowerKey = key.toLowerCase();
            if (lowerKey.contains("password")
                    || lowerKey.contains("token")
                    || lowerKey.contains("apikey")
                    || lowerKey.contains("api_key")
                    || lowerKey.contains("secret")) {
                sanitized.set(key, "***");
                continue;
            }
            if (value instanceof CharSequence text) {
                sanitized.set(key, StrUtil.subPre(text.toString(), MAX_ARGUMENT_VALUE_LENGTH));
            } else {
                sanitized.set(key, value);
            }
        }
        return sanitized;
    }
}
