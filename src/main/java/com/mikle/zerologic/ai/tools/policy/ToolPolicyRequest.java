package com.mikle.zerologic.ai.tools.policy;

import cn.hutool.json.JSONObject;
import com.mikle.zerologic.model.enums.ToolRiskLevelEnum;
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

    private String callSource;

    private JSONObject arguments;
}
