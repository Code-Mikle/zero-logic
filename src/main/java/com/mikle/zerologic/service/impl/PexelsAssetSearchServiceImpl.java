package com.mikle.zerologic.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.mikle.zerologic.config.AssetProperties;
import com.mikle.zerologic.model.entity.ToolCallRecord;
import com.mikle.zerologic.model.enums.ToolCallStatusEnum;
import com.mikle.zerologic.model.enums.ToolCategoryEnum;
import com.mikle.zerologic.model.enums.ToolRiskLevelEnum;
import com.mikle.zerologic.service.AssetSearchService;
import com.mikle.zerologic.service.ToolCallRecordService;
import com.mikle.zerologic.workflow.generation.asset.AssetResource;
import com.mikle.zerologic.workflow.generation.asset.AssetSearchRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PexelsAssetSearchServiceImpl implements AssetSearchService {

    private static final String PEXELS_API_URL = "https://api.pexels.com/v1/search";

    private static final int MAX_RECORD_TEXT_LENGTH = 2000;

    @Resource
    private AssetProperties assetProperties;

    @Resource
    private ToolCallRecordService toolCallRecordService;

    @Override
    public List<AssetResource> search(AssetSearchRequest request) {
        if (request == null || StrUtil.isBlank(request.getKeyword())) {
            return List.of();
        }
        if (StrUtil.isBlank(assetProperties.getPexelsApiKey())) {
            log.info("Asset search skipped because generation.asset.pexels-api-key is empty");
            return List.of();
        }

        long startTime = System.currentTimeMillis();
        String status = ToolCallStatusEnum.SUCCESS.getValue();
        String errorMessage = null;
        List<AssetResource> assets = new ArrayList<>();
        try {
            int limit = normalizeLimit(request.getLimit());
            try (HttpResponse response = HttpRequest.get(PEXELS_API_URL)
                    .header("Authorization", assetProperties.getPexelsApiKey())
                    .form("query", request.getKeyword())
                    .form("per_page", limit)
                    .form("page", 1)
                    .timeout(assetProperties.getTimeoutMs())
                    .execute()) {
                if (!response.isOk()) {
                    status = ToolCallStatusEnum.FAILED.getValue();
                    errorMessage = "Pexels API HTTP " + response.getStatus();
                    return List.of();
                }
                JSONObject result = JSONUtil.parseObj(response.body());
                JSONArray photos = result.getJSONArray("photos");
                if (photos == null || photos.isEmpty()) {
                    return List.of();
                }
                int actualCount = Math.min(limit, photos.size());
                for (int i = 0; i < actualCount; i++) {
                    JSONObject photo = photos.getJSONObject(i);
                    JSONObject src = photo.getJSONObject("src");
                    if (src == null) {
                        continue;
                    }
                    String imageUrl = src.getStr("large", src.getStr("medium"));
                    if (StrUtil.isBlank(imageUrl)) {
                        continue;
                    }
                    String title = photo.getStr("alt", request.getKeyword());
                    assets.add(AssetResource.builder()
                            .keyword(request.getKeyword())
                            .title(title)
                            .description(title)
                            .url(imageUrl)
                            .source("pexels")
                            .build());
                }
                return assets;
            }
        } catch (Exception e) {
            status = ToolCallStatusEnum.FAILED.getValue();
            errorMessage = StrUtil.blankToDefault(e.getMessage(), e.getClass().getSimpleName());
            log.warn("Asset search failed, keyword={}", request.getKeyword(), e);
            return List.of();
        } finally {
            saveToolCallRecord(request, assets, status, errorMessage, System.currentTimeMillis() - startTime);
        }
    }

    private int normalizeLimit(Integer limit) {
        int configuredLimit = assetProperties.getPerKeywordLimit();
        int requestedLimit = limit == null || limit <= 0 ? configuredLimit : limit;
        return Math.max(1, Math.min(requestedLimit, configuredLimit));
    }

    private void saveToolCallRecord(AssetSearchRequest request, List<AssetResource> assets,
                                    String status, String errorMessage, long durationMs) {
        try {
            JSONObject arguments = new JSONObject();
            arguments.set("keyword", request.getKeyword());
            arguments.set("limit", request.getLimit());

            JSONObject result = new JSONObject();
            result.set("keyword", request.getKeyword());
            result.set("source", "pexels");
            result.set("count", assets == null ? 0 : assets.size());

            ToolCallRecord record = ToolCallRecord.builder()
                    .taskId(request.getTaskId())
                    .appId(request.getAppId())
                    .userId(request.getUserId())
                    .toolName("asset_search")
                    .displayName("Asset Search")
                    .toolCategory(ToolCategoryEnum.KNOWLEDGE.getValue())
                    .riskLevel(ToolRiskLevelEnum.LOW.getValue())
                    .callSource("generate")
                    .status(status)
                    .argumentsJson(StrUtil.subPre(JSONUtil.toJsonStr(arguments), MAX_RECORD_TEXT_LENGTH))
                    .resultSummary(StrUtil.subPre(JSONUtil.toJsonStr(result), MAX_RECORD_TEXT_LENGTH))
                    .errorMessage(StrUtil.subPre(errorMessage, MAX_RECORD_TEXT_LENGTH))
                    .durationMs(durationMs)
                    .build();
            toolCallRecordService.save(record);
        } catch (Exception e) {
            log.warn("Failed to save asset search audit record, keyword={}", request.getKeyword(), e);
        }
    }
}
