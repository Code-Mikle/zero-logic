package com.mikle.zerologic.generation.tool.execution;

import com.mikle.zerologic.generation.tool.model.enums.ToolCallStatusEnum;

/**
 * 工具内部执行结果，显式区分执行状态和返回给模型的消息。
 */
public record ToolActionResult(ToolCallStatusEnum status, String message) {

    public static ToolActionResult success(String message) {
        return new ToolActionResult(ToolCallStatusEnum.SUCCESS, message);
    }

    public static ToolActionResult failed(String message) {
        return new ToolActionResult(ToolCallStatusEnum.FAILED, message);
    }
}
