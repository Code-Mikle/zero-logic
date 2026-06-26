package com.mikle.zerologic.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "tool.policy")
public class ToolPolicyProperties {

    @Min(1000)
    @Max(2_000_000)
    private int maxWriteChars = 300_000;

    @Min(1000)
    @Max(2_000_000)
    private int maxModifyChars = 300_000;

    private List<String> protectedFiles = new ArrayList<>(List.of(
            "package.json",
            "package-lock.json",
            "pnpm-lock.yaml",
            "yarn.lock",
            "vite.config.js",
            "vite.config.ts",
            "tsconfig.json",
            "tsconfig.app.json",
            "tsconfig.node.json",
            ".env",
            ".env.local",
            ".npmrc"
    ));

    private List<String> protectedDirs = new ArrayList<>(List.of(
            "node_modules",
            "dist",
            "build",
            ".git",
            "target"
    ));
}
