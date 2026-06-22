package com.mikle.zerologic.document.parser;

import com.mikle.zerologic.document.DocumentParser;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.Set;

@Component
public class MarkdownDocumentParser implements DocumentParser {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "md", "markdown"
    );

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "text/markdown",
            "text/plain",
            "application/octet-stream"
    );

    @Override
    public boolean supports(String extension, String contentType) {

        if (extension == null || !SUPPORTED_EXTENSIONS.contains(extension.toLowerCase())) {
            return false;
        }

        return contentType == null
                || contentType.isBlank()
                || SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase());
    }

    @Override
    public String parse(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return "";
        }

        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Markdown 文件解析失败: " + file.getOriginalFilename(), e);
        }
    }
}
