package com.mikle.zerologic.monitor;

import com.mikle.zerologic.mapper.GenerationTaskMapper;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * AI 模型监听器
 */
@Component
@Slf4j
public class AiModelMonitorListener implements ChatModelListener {

    // 用于存储请求开始时间的键
    private static final String REQUEST_START_TIME_KEY = "request_start_time";
    // 用于监控上下文传递（因为请求和响应事件的触发不是同一个线程）
    private static final String MONITOR_CONTEXT_KEY = "monitor_context";

    @Resource
    private AiModelMetricsCollector aiModelMetricsCollector;

    @Resource
    private GenerationTaskMapper generationTaskMapper;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        // 获取当前时间戳，但未做任何处理
        requestContext.attributes().put(REQUEST_START_TIME_KEY, Instant.now());
        // 从监控上下文中获取信息
        MonitorContext monitorContext = MonitorContextHolder.getContext();
        String userId = getUserId(monitorContext);
        String appId = getAppId(monitorContext);
        if (monitorContext != null) {
            requestContext.attributes().put(MONITOR_CONTEXT_KEY, monitorContext);
        }
        // 获取模型名称
        String modelName = requestContext.chatRequest().modelName();
        // 记录请求指标
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "started");
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        // 从属性中获取监控信息（由 onRequest 方法存储）
        Map<Object, Object> attributes = responseContext.attributes();
        // 从监控上下文中获取信息
        MonitorContext context = (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY);
        String userId = getUserId(context);
        String appId = getAppId(context);
        // 获取模型名称
        String modelName = responseContext.chatResponse().modelName();
        // 记录成功请求
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "success");
        // 记录响应时间
        recordResponseTime(attributes, userId, appId, modelName);
        // 记录 Token 使用情况
        recordTokenUsage(responseContext, context, userId, appId, modelName);
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        // 从监控上下文中获取信息
        Map<Object, Object> attributes = errorContext.attributes();
        MonitorContext context = (MonitorContext) attributes.getOrDefault(MONITOR_CONTEXT_KEY,
                MonitorContextHolder.getContext());
        String userId = getUserId(context);
        String appId = getAppId(context);
        // 获取模型名称和错误类型
        String modelName = errorContext.chatRequest().modelName();
        String errorMessage = errorContext.error().getMessage();
        // 记录失败请求
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "error");
        aiModelMetricsCollector.recordError(userId, appId, modelName, errorMessage);
        // 记录响应时间（即使是错误响应）
        recordResponseTime(attributes, userId, appId, modelName);
    }

    /**
     * 记录响应时间
     */
    private void recordResponseTime(Map<Object, Object> attributes, String userId, String appId, String modelName) {
        Instant startTime = (Instant) attributes.get(REQUEST_START_TIME_KEY);
        if (startTime == null) {
            return;
        }
        Duration responseTime = Duration.between(startTime, Instant.now());
        aiModelMetricsCollector.recordResponseTime(userId, appId, modelName, responseTime);
    }

    /**
     * 记录Token使用情况
     */
    private void recordTokenUsage(ChatModelResponseContext responseContext, MonitorContext context,
                                  String userId, String appId, String modelName) {
        TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();
        Long taskId = parseTaskId(context);
        if (tokenUsage == null) {
            log.warn("AI TokenUsage 为空: taskId={}, userId={}, appId={}, modelName={}. "
                            + "请确认流式请求已携带 stream_options.include_usage=true，且模型供应商返回了 usage chunk",
                    taskId, userId, appId, modelName);
            return;
        }

        Integer inputTokenCount = tokenUsage.inputTokenCount();
        Integer outputTokenCount = tokenUsage.outputTokenCount();
        Integer totalTokenCount = tokenUsage.totalTokenCount();
        log.info("AI TokenUsage 获取成功: taskId={}, userId={}, appId={}, modelName={}, input={}, output={}, total={}",
                taskId, userId, appId, modelName, inputTokenCount, outputTokenCount, totalTokenCount);

        recordMetricTokenUsage(userId, appId, modelName, "input", inputTokenCount);
        recordMetricTokenUsage(userId, appId, modelName, "output", outputTokenCount);
        recordMetricTokenUsage(userId, appId, modelName, "total", totalTokenCount);
        if (taskId != null && totalTokenCount != null && totalTokenCount > 0) {
            int updatedRows = generationTaskMapper.addTokenUsage(taskId, totalTokenCount.longValue());
            log.info("generation_task tokenUsage 累加完成: taskId={}, totalTokenCount={}, updatedRows={}",
                    taskId, totalTokenCount, updatedRows);
        } else {
            log.warn("generation_task tokenUsage 未写入: taskId={}, totalTokenCount={}", taskId, totalTokenCount);
        }
    }

    private void recordMetricTokenUsage(String userId, String appId, String modelName,
                                        String tokenType, Integer tokenCount) {
        if (tokenCount != null && tokenCount > 0) {
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, tokenType, tokenCount);
        }
    }

    private Long parseTaskId(MonitorContext context) {
        if (context == null || context.getTaskId() == null) {
            return null;
        }
        try {
            return Long.valueOf(context.getTaskId());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getUserId(MonitorContext context) {
        return context == null || context.getUserId() == null ? "unknown" : context.getUserId();
    }

    private String getAppId(MonitorContext context) {
        return context == null || context.getAppId() == null ? "unknown" : context.getAppId();
    }
}
