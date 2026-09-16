package com.mikle.zerologic.generation.asset.service;

import com.mikle.zerologic.generation.asset.model.AssetPlan;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AssetPlanService {

    @SystemMessage(fromResource = "prompt/asset-plan-system-prompt.txt")
    AssetPlan plan(@UserMessage String request);
}
