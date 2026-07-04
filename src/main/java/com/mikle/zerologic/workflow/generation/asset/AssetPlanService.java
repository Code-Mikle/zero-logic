package com.mikle.zerologic.workflow.generation.asset;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AssetPlanService {

    @SystemMessage(fromResource = "prompt/asset-plan-system-prompt.txt")
    AssetPlan plan(@UserMessage String request);
}
