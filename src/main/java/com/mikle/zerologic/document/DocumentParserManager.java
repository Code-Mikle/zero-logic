package com.mikle.zerologic.document;

import cn.hutool.core.io.FileUtil;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Component
public class DocumentParserManager {

    @Resource
    private List<DocumentParser> documentParsers;

    /**
     * 也可以使用 Stream 过滤，但可读性差，且不方便加日志或调试
     */
    public String documentParse(MultipartFile file) {
        // 强制校验 file 变量不能为空（null），如果为空，则立即抛出带有明确提示信息的空指针异常
        Objects.requireNonNull(file, "file must not be null");

        String extName = FileUtil.extName(file.getOriginalFilename());
        String contentType = file.getContentType();

        for (DocumentParser documentParser : documentParsers) {
            if (documentParser.supports(extName, contentType)) {
                return documentParser.parse(file);
            }
        }

        throw new BusinessException(
                ErrorCode.PARAMS_ERROR,
                "不支持的文件类型: extension=" + extName + ", contentType=" + contentType
        );
    }

}
