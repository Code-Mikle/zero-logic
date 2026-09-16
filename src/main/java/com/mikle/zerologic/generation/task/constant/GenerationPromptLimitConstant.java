package com.mikle.zerologic.generation.task.constant;

/**
 * 生成任务提示词与模型消息限制。
 */
public final class GenerationPromptLimitConstant {

    public static final int MAX_USER_PROMPT_LENGTH = 1_000;

    public static final int MAX_MODEL_MESSAGE_LENGTH = 22_000;

    private GenerationPromptLimitConstant() {
    }
}
