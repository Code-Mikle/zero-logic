package com.mikle.zerologic.workflow.generation.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mikle.zerologic.config.AssetProperties;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.service.GenerationTaskProgressService;
import com.mikle.zerologic.workflow.generation.GenerationWorkflowContext;
import com.mikle.zerologic.workflow.generation.asset.AssetPlan;
import com.mikle.zerologic.workflow.generation.asset.AssetPlanService;
import com.mikle.zerologic.workflow.generation.asset.AssetSearchTask;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Component
public class AssetPlanNode {

    private static final int MAX_KEYWORD_LENGTH = 120;

    @Resource
    private AssetProperties assetProperties;

    @Resource
    private AssetPlanService assetPlanService;

    @Resource
    private GenerationTaskProgressService taskProgressService;

    public AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            GenerationWorkflowContext context = GenerationWorkflowContext.getContext(state);
            if (context == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成工作流上下文不存在");
            }

            AssetPlan plan = buildPlan(context);
            context.setAssetPlan(plan);
            context.setCurrentStep("asset_plan");
            taskProgressService.updateStep(context.getTaskId(), "asset_plan");
            log.info("Asset plan finished: appId={}, enabled={}, reason={}, tasks={}",
                    context.getAppId(), plan.isEnabled(), plan.getReason(),
                    plan.getSearchTasks() == null ? 0 : plan.getSearchTasks().size());
            return GenerationWorkflowContext.saveContext(context);
        });
    }

    private AssetPlan buildPlan(GenerationWorkflowContext context) {
        if (!assetProperties.isEnabled()) {
            return disabled("asset feature disabled");
        }
        if (!assetProperties.isAiPlanningEnabled()) {
            return disabled("ai asset planning disabled");
        }

        String prompt = StrUtil.blankToDefault(context.getOriginalMessage(), context.getMessage());
        if (StrUtil.isBlank(prompt) && StrUtil.isBlank(context.getRagContext())) {
            return disabled("empty planning context");
        }

        try {
            AssetPlan rawPlan = assetPlanService.plan(buildPlanningRequest(context, prompt));
            return sanitizePlan(rawPlan);
        } catch (Exception e) {
            log.warn("AI asset planning failed, appId={}", context.getAppId(), e);
            return disabled("ai asset planning failed");
        }
    }

    private String buildPlanningRequest(GenerationWorkflowContext context, String prompt) {
        return """
                Current user request:
                %s

                Retrieved application context:
                %s

                Planning task:
                Decide whether this generation turn needs external visual assets.
                If the current request is short, follow-up, or about a broken/missing/unsuitable image,
                use the retrieved application context to infer the website theme and plan replacement image keywords.

                Planning constraints:
                - maxKeywords: %d
                - perKeywordLimit: %d
                - only content image search is supported
                - return compact JSON only
                - reason must be no longer than 80 characters
                - keyword must be English and no longer than 8 words
                """.formatted(
                StrUtil.subPre(StrUtil.blankToDefault(prompt, ""), assetProperties.getMaxPlanPromptChars()),
                StrUtil.subPre(StrUtil.blankToDefault(context.getRagContext(), ""), assetProperties.getMaxPlanContextChars()),
                assetProperties.getMaxKeywords(),
                assetProperties.getPerKeywordLimit());
    }

    private AssetPlan sanitizePlan(AssetPlan rawPlan) {
        if (rawPlan == null || !Boolean.TRUE.equals(rawPlan.getEnabled())) {
            return disabled(rawPlan == null ? "ai returned empty plan" : rawPlan.getReason());
        }

        if (CollUtil.isEmpty(rawPlan.getSearchTasks())) {
            return disabled("ai returned no search tasks");
        }

        List<AssetSearchTask> tasks = rawPlan.getSearchTasks().stream()
                .filter(Objects::nonNull)
                .map(this::sanitizeTask)
                .filter(Objects::nonNull)
                .distinct()
                .limit(assetProperties.getMaxKeywords())
                .toList();

        if (tasks.isEmpty()) {
            return disabled("ai returned no valid search tasks");
        }

        return AssetPlan.builder()
                .enabled(true)
                .reason(StrUtil.subPre(StrUtil.blankToDefault(rawPlan.getReason(), "ai planned visual assets"), 200))
                .searchTasks(tasks)
                .build();
    }

    private AssetSearchTask sanitizeTask(AssetSearchTask task) {
        String keyword = normalizeKeyword(task.getKeyword());
        if (StrUtil.isBlank(keyword)) {
            return null;
        }
        return AssetSearchTask.builder()
                .keyword(keyword)
                .scene(StrUtil.subPre(StrUtil.blankToDefault(task.getScene(), "webpage visual material"), 80))
                .limit(normalizeLimit(task.getLimit()))
                .build();
    }

    private String normalizeKeyword(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return "";
        }
        String normalized = keyword
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('"', ' ')
                .replace('\'', ' ')
                .trim()
                .replaceAll("\\s+", " ");
        return StrUtil.subPre(normalized, MAX_KEYWORD_LENGTH);
    }

    private Integer normalizeLimit(Integer limit) {
        int requested = limit == null || limit <= 0 ? assetProperties.getPerKeywordLimit() : limit;
        return Math.max(1, Math.min(requested, assetProperties.getPerKeywordLimit()));
    }

    private AssetPlan disabled(String reason) {
        return AssetPlan.builder()
                .enabled(false)
                .reason(StrUtil.blankToDefault(reason, "asset planning disabled"))
                .searchTasks(List.of())
                .build();
    }
}
