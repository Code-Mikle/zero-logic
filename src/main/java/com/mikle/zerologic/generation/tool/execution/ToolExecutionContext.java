package com.mikle.zerologic.generation.tool.execution;

import com.mikle.zerologic.generation.tool.model.enums.ToolCallSourceEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToolExecutionContext {

    private Long taskId;

    private Long appId;

    private Long userId;

    private ToolCallSourceEnum callSource;
}
