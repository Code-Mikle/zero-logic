package com.mikle.zerologic.rag;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * 向量和 JSON 转换
 */
public final class EmbeddingJsonUtils {

    private EmbeddingJsonUtils() {
    }

    public static String toJson(List<Double> embedding) {
        return JSONUtil.toJsonStr(embedding);
    }

    public static List<Double> fromJson(String json) {
        JSONArray array = JSONUtil.parseArray(json);
        List<Double> result = new ArrayList<>();
        for (Object item : array) {
            result.add(Double.valueOf(item.toString()));
        }
        return result;
    }
}
