package com.mikle.zerologic.ai.tools;

import cn.hutool.json.JSONObject;
import com.mikle.zerologic.model.enums.ToolCategoryEnum;
import com.mikle.zerologic.model.enums.ToolRiskLevelEnum;

/**
 * 工具基类
 * 定义所有工具的通用接口
 */
public abstract class BaseTool {

    /**
     * 获取工具的英文名称（对应方法名）
     *
     * @return 工具英文名称
     */
    public abstract String getToolName();

    /**
     * 获取工具的中文显示名称
     *
     * @return 工具中文名称
     */
    public abstract String getDisplayName();

    public ToolCategoryEnum getCategory() {
        return ToolCategoryEnum.FILE;
    }

    public ToolRiskLevelEnum getRiskLevel() {
        return ToolRiskLevelEnum.LOW;
    }

    public boolean isMutating() {
        return false;
    }

    public boolean isEnabled() {
        return true;
    }

    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .toolName(getToolName())
                .displayName(getDisplayName())
                .category(getCategory())
                .riskLevel(getRiskLevel())
                .mutating(isMutating())
                .enabled(isEnabled())
                .build();
    }

    /**
     * 生成工具请求时的返回值（显示给用户）
     *
     * @return 工具请求显示内容
     */
    public String generateToolRequestResponse() {
        return String.format("\n\n[选择工具] %s\n\n", getDisplayName());
    }

    /**
     * 生成工具执行结果格式（保存到数据库）
     *
     * @param arguments 工具执行参数
     * @return 格式化的工具执行结果
     */
    public abstract String generateToolExecutedResult(JSONObject arguments);
}
