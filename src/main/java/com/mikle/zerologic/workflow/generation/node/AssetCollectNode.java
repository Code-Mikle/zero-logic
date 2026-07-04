package com.mikle.zerologic.workflow.generation.node;

import com.mikle.zerologic.config.AssetProperties;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.service.AssetSearchService;
import com.mikle.zerologic.service.GenerationTaskProgressService;
import com.mikle.zerologic.workflow.generation.GenerationWorkflowContext;
import com.mikle.zerologic.workflow.generation.asset.AssetPlan;
import com.mikle.zerologic.workflow.generation.asset.AssetResource;
import com.mikle.zerologic.workflow.generation.asset.AssetSearchRequest;
import com.mikle.zerologic.workflow.generation.asset.AssetSearchTask;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Component
public class AssetCollectNode {

    @Resource
    private AssetProperties assetProperties;

    @Resource
    private AssetSearchService assetSearchService;

    @Resource
    private GenerationTaskProgressService taskProgressService;

    public AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            GenerationWorkflowContext context = GenerationWorkflowContext.getContext(state);
            if (context == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成工作流上下文不存在");
            }

            List<AssetResource> resources = collect(context);
            context.setAssetResources(resources);
            context.setCurrentStep("asset_collect");
            taskProgressService.updateStep(context.getTaskId(), "asset_collect");
            log.info("Asset collection finished: appId={}, count={}", context.getAppId(), resources.size());
            return GenerationWorkflowContext.saveContext(context);
        });
    }

    private List<AssetResource> collect(GenerationWorkflowContext context) {
        AssetPlan plan = context.getAssetPlan();
        if (plan == null || !plan.isEnabled()) {
            return List.of();
        }

        List<AssetResource> resources = new ArrayList<>();
        for (AssetSearchTask task : plan.getSearchTasks()) {
            if (resources.size() >= assetProperties.getMaxAssets()) {
                break;
            }
            try {
                List<AssetResource> found = assetSearchService.search(AssetSearchRequest.builder()
                        .taskId(context.getTaskId())
                        .appId(context.getAppId())
                        .userId(context.getUserId())
                        .keyword(task.getKeyword())
                        .limit(task.getLimit())
                        .build());
                for (AssetResource resource : found) {
                    if (resources.size() >= assetProperties.getMaxAssets()) {
                        break;
                    }
                    resources.add(resource);
                }
            } catch (Exception e) {
                log.warn("Asset search skipped after error, appId={}, keyword={}",
                        context.getAppId(), task.getKeyword(), e);
            }
        }
        return resources;
    }
}
