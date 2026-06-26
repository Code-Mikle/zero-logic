package com.mikle.zerologic.core.build;

import com.mikle.zerologic.constant.AppConstant;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.model.enums.CodeGenTypeEnum;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class GeneratedProjectPathResolver {

    public Path resolve(Long appId, CodeGenTypeEnum codeGenType) {
        if (appId == null || appId <= 0 || codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成项目路径参数错误");
        }
        Path root = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR).toAbsolutePath().normalize();
        Path target = root.resolve(codeGenType.getValue() + "_" + appId).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "生成项目路径越界");
        }
        return target;
    }
}
