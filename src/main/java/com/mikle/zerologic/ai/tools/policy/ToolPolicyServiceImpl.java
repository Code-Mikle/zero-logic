package com.mikle.zerologic.ai.tools.policy;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.mikle.zerologic.ai.tools.ProjectToolPathResolver;
import com.mikle.zerologic.config.ToolPolicyProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ToolPolicyServiceImpl implements ToolPolicyService {

    @Resource
    private ToolPolicyProperties properties;

    @Resource
    private ProjectToolPathResolver pathResolver;

    @Override
    public ToolPolicyResult check(ToolPolicyRequest request) {
        if (request == null || request.getOperation() == null) {
            return ToolPolicyResult.reject("工具策略请求为空");
        }
        ToolOperationEnum operation = request.getOperation();
        if (operation == ToolOperationEnum.READ || operation == ToolOperationEnum.CONTROL) {
            return ToolPolicyResult.allow();
        }
        JSONObject arguments = request.getArguments();
        String relativePath = getRelativePath(arguments);
        if (StrUtil.isBlank(relativePath)) {
            return ToolPolicyResult.reject("写入、修改或删除工具必须提供相对路径");
        }
        if (operation == ToolOperationEnum.DELETE && "repair".equals(request.getCallSource())) {
            return ToolPolicyResult.reject("自动修复阶段不允许删除文件");
        }
        ToolPolicyResult contentLimitResult = checkContentLimit(operation, arguments);
        if (!contentLimitResult.isAllowed()) {
            return contentLimitResult;
        }
        if (isProtectedDirectoryPath(relativePath)) {
            return ToolPolicyResult.reject("受保护目录不允许执行该工具操作：" + relativePath);
        }
        if (!isProtectedFilePath(relativePath)) {
            return ToolPolicyResult.allow();
        }
        if (operation == ToolOperationEnum.WRITE && "generate".equals(request.getCallSource())
                && isCreatingProtectedFile(request.getAppId(), relativePath)) {
            return ToolPolicyResult.allow();
        }
        return ToolPolicyResult.reject("受保护路径不允许执行该工具操作：" + relativePath);
    }

    private ToolPolicyResult checkContentLimit(ToolOperationEnum operation, JSONObject arguments) {
        if (arguments == null) {
            return ToolPolicyResult.allow();
        }
        if (operation == ToolOperationEnum.WRITE) {
            String content = arguments.getStr("content");
            if (content != null && content.length() > properties.getMaxWriteChars()) {
                return ToolPolicyResult.reject("写入内容过大，已超过 " + properties.getMaxWriteChars() + " 字符");
            }
        }
        if (operation == ToolOperationEnum.MODIFY) {
            String newContent = arguments.getStr("newContent");
            if (newContent != null && newContent.length() > properties.getMaxModifyChars()) {
                return ToolPolicyResult.reject("修改内容过大，已超过 " + properties.getMaxModifyChars() + " 字符");
            }
        }
        return ToolPolicyResult.allow();
    }

    private String getRelativePath(JSONObject arguments) {
        if (arguments == null) {
            return null;
        }
        String filePath = arguments.getStr("relativeFilePath");
        if (StrUtil.isNotBlank(filePath)) {
            return filePath;
        }
        return arguments.getStr("relativeDirPath");
    }

    private boolean isCreatingProtectedFile(Long appId, String relativePath) {
        try {
            Path target = pathResolver.resolve(appId, relativePath);
            return !Files.exists(target);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isProtectedDirectoryPath(String relativePath) {
        String normalizedPath = normalizePath(relativePath);
        if (StrUtil.isBlank(normalizedPath)) {
            return false;
        }
        Set<String> protectedDirs = properties.getProtectedDirs().stream()
                .map(this::normalizePath)
                .collect(Collectors.toSet());
        String[] parts = Arrays.stream(normalizedPath.split("/"))
                .filter(StrUtil::isNotBlank)
                .toArray(String[]::new);
        for (String part : parts) {
            if (protectedDirs.contains(part)) {
                return true;
            }
        }
        if (parts.length == 0) {
            return false;
        }
        return false;
    }

    private boolean isProtectedFilePath(String relativePath) {
        String normalizedPath = normalizePath(relativePath);
        if (StrUtil.isBlank(normalizedPath)) {
            return false;
        }
        Set<String> protectedFiles = properties.getProtectedFiles().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        String[] parts = Arrays.stream(normalizedPath.split("/"))
                .filter(StrUtil::isNotBlank)
                .toArray(String[]::new);
        if (parts.length == 0) {
            return false;
        }
        return protectedFiles.contains(parts[parts.length - 1]);
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.replace('\\', '/').trim().toLowerCase();
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }
}
