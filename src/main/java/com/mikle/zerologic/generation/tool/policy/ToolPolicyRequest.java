package com.mikle.zerologic.generation.tool.policy;

import cn.hutool.json.JSONObject;
import com.mikle.zerologic.generation.tool.model.enums.ToolCallSourceEnum;
import com.mikle.zerologic.generation.tool.model.enums.ToolRiskLevelEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToolPolicyRequest {

    private String toolName;

    private ToolOperationEnum operation;

    private ToolRiskLevelEnum riskLevel;

    private Long appId;

    private Long taskId;

    private Long userId;

    private ToolCallSourceEnum callSource;

    private JSONObject arguments;
}
