package com.mikle.zerologic.ai.tools;

import com.mikle.zerologic.ai.tools.policy.ToolOperationEnum;
import com.mikle.zerologic.model.enums.ToolCategoryEnum;
import com.mikle.zerologic.model.enums.ToolRiskLevelEnum;
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
