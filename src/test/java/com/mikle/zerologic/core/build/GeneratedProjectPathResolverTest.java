package com.mikle.zerologic.core.build;

import com.mikle.zerologic.constant.AppConstant;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedProjectPathResolverTest {

    private final GeneratedProjectPathResolver resolver = new GeneratedProjectPathResolver();

    @Test
    void shouldResolveProjectUnderOutputRoot() {
        Path result = resolver.resolve(123L, CodeGenTypeEnum.VUE_PROJECT);
        Path root = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR).toAbsolutePath().normalize();

        assertTrue(result.startsWith(root));
        assertEquals("vue_project_123", result.getFileName().toString());
    }

    @Test
    void shouldRejectInvalidAppId() {
        assertThrows(BusinessException.class,
                () -> resolver.resolve(0L, CodeGenTypeEnum.HTML));
    }
}
