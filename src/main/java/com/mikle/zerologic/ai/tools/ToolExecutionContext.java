package com.mikle.zerologic.ai.tools;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToolExecutionContext {

    private Long taskId;

    private Long appId;

    private Long userId;

    /**
     * generate / repair / manual
     */
    private String callSource;
}
