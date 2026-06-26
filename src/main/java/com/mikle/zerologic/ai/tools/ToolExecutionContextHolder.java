package com.mikle.zerologic.ai.tools;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ToolExecutionContextHolder {

    private static final ThreadLocal<ToolExecutionContext> LOCAL_CONTEXT = new ThreadLocal<>();

    private static final Map<Long, ToolExecutionContext> APP_CONTEXT_MAP = new ConcurrentHashMap<>();

    private ToolExecutionContextHolder() {
    }

    public static void set(ToolExecutionContext context) {
        if (context == null) {
            return;
        }
        LOCAL_CONTEXT.set(context);
        if (context.getAppId() != null) {
            APP_CONTEXT_MAP.put(context.getAppId(), context);
        }
    }

    public static ToolExecutionContext get(Long appId) {
        ToolExecutionContext localContext = LOCAL_CONTEXT.get();
        if (localContext != null) {
            return localContext;
        }
        return appId == null ? null : APP_CONTEXT_MAP.get(appId);
    }

    public static void clear(Long appId) {
        LOCAL_CONTEXT.remove();
        if (appId != null) {
            APP_CONTEXT_MAP.remove(appId);
        }
    }
}
