package com.mikle.zerologic.generation.tool.execution;

import com.mikle.zerologic.generation.tool.model.enums.ToolCategoryEnum;
import com.mikle.zerologic.generation.tool.model.enums.ToolRiskLevelEnum;
import com.mikle.zerologic.generation.tool.policy.ToolOperationEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToolDefinition {

    private String toolName;

    private String displayName;

    private ToolCategoryEnum category;

    private ToolRiskLevelEnum riskLevel;

    private ToolOperationEnum operation;

    private boolean mutating;

    private boolean enabled;
}
