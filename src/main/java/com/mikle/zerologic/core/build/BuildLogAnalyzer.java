package com.mikle.zerologic.core.build;

import cn.hutool.core.util.StrUtil;
import com.mikle.zerologic.config.RepairProperties;
import com.mikle.zerologic.core.build.model.BuildDiagnosis;
import com.mikle.zerologic.core.build.model.BuildResult;
import com.mikle.zerologic.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BuildLogAnalyzer {
    private static final Pattern FILE_PATTERN = Pattern.compile(
            "(?m)(?:^|[\\s(])((?:src|public)/[^\\s:()]+\\.(?:vue|ts|tsx|js|jsx|css|scss|json))(?::\\d+(?::\\d+)?)?");

    @Resource
    private RepairProperties repairProperties;

    public BuildDiagnosis analyze(CodeGenTypeEnum codeGenType, BuildResult result) {
        String log = StrUtil.subPre(result == null ? null : result.getLogText(),
                repairProperties.getMaxBuildLogChars());
        boolean repairable = repairProperties.isEnabled()
                && repairProperties.getMaxAttempts() > 0
                && codeGenType == CodeGenTypeEnum.VUE_PROJECT
                && result != null
                && !Boolean.TRUE.equals(result.getSuccess())
                && !Boolean.TRUE.equals(result.getTimedOut())
                && result.getCommand() != null
                && result.getCommand().contains(" run build");

        Set<String> files = new LinkedHashSet<>();
        Matcher matcher = FILE_PATTERN.matcher(StrUtil.blankToDefault(log, ""));
        while (matcher.find() && files.size() < 10) {
            files.add(matcher.group(1).replace('\\', '/'));
        }
        return BuildDiagnosis.builder()
                .repairable(repairable)
                .summary(extractSummary(log))
                .suspectedFiles(List.copyOf(files))
                .build();
    }

    private String extractSummary(String log) {
        if (StrUtil.isBlank(log)) {
            return "Build failed without diagnostic output.";
        }
        List<String> important = log.lines()
                .filter(line -> line.contains("error TS") || line.contains("ERROR")
                        || line.contains("Error:") || line.contains("failed"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .distinct()
                .limit(20)
                .toList();
        String summary = important.isEmpty() ? log : String.join("\n", important);
        return StrUtil.subPre(summary, repairProperties.getMaxBuildLogChars());
    }
}
