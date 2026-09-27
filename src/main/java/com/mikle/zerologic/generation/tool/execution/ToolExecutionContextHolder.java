package com.mikle.zerologic.generation.tool.execution;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ToolExecutionContextHolder {

    private static final Map<Long, ToolExecutionContext> APP_CONTEXT_MAP = new ConcurrentHashMap<>();

    public static void set(ToolExecutionContext context) {
        if (context == null || context.getAppId() == null) {
            return;
        }
        // TokenStream 回调可能切换线程，ThreadLocal 无法稳定传递，还可能在线程池中残留旧任务上下文。
        // 生成流程已按 appId 互斥，因此直接按 appId 管理跨线程上下文。
        APP_CONTEXT_MAP.put(context.getAppId(), context);
    }

    public static ToolExecutionContext get(Long appId) {
        return appId == null ? null : APP_CONTEXT_MAP.get(appId);
    }

    public static void clear(Long appId) {
        if (appId != null) {
            APP_CONTEXT_MAP.remove(appId);
        }
    }
}
