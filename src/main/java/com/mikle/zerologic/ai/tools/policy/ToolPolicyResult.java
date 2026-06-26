package com.mikle.zerologic.ai.tools.policy;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolPolicyResult {

    private boolean allowed;

    private String reason;

    public static ToolPolicyResult allow() {
        return new ToolPolicyResult(true, null);
    }

    public static ToolPolicyResult reject(String reason) {
        return new ToolPolicyResult(false, reason);
    }
}
