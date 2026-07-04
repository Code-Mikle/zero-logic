package com.mikle.zerologic.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "generation.asset")
public class AssetProperties {

    private boolean enabled = true;

    private boolean aiPlanningEnabled = true;

    @Min(1)
    @Max(5)
    private int maxKeywords = 3;

    @Min(1)
    @Max(5)
    private int perKeywordLimit = 2;

    @Min(1)
    @Max(12)
    private int maxAssets = 6;

    @Min(1000)
    @Max(30000)
    private int timeoutMs = 8000;

    @Min(200)
    @Max(8000)
    private int maxPlanPromptChars = 2000;

    @Min(200)
    @Max(12000)
    private int maxPlanContextChars = 4000;

    @Min(128)
    @Max(2048)
    private int planMaxTokens = 800;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double planTemperature = 0.1;

    private String pexelsApiKey = "";
}
