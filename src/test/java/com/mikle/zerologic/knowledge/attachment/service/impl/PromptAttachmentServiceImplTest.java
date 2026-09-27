package com.mikle.zerologic.knowledge.attachment.service.impl;

import com.mikle.zerologic.app.mapper.AppMapper;
import com.mikle.zerologic.exception.BusinessException;
import com.mybatisflex.core.query.QueryWrapper;
import com.mikle.zerologic.knowledge.attachment.mapper.PromptAttachmentMapper;
import com.mikle.zerologic.knowledge.attachment.model.enums.AttachmentStatusEnum;
import com.mikle.zerologic.knowledge.attachment.model.entity.PromptAttachment;
import com.mikle.zerologic.knowledge.attachment.model.vo.PromptAttachmentVO;
import com.mikle.zerologic.knowledge.document.parser.DocumentParserManager;
import com.mikle.zerologic.user.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromptAttachmentServiceImplTest {

    private DocumentParserManager documentParserManager;
    private PromptAttachmentMapper attachmentMapper;
    private AppMapper appMapper;
    private PromptAttachmentServiceImpl service;
    private User loginUser;

    @BeforeEach
    void setUp() {
        documentParserManager = mock(DocumentParserManager.class);
        attachmentMapper = mock(PromptAttachmentMapper.class);
        appMapper = mock(AppMapper.class);
        service = spy(new PromptAttachmentServiceImpl());
        ReflectionTestUtils.setField(service, "documentParserManager", documentParserManager);
        ReflectionTestUtils.setField(service, "appMapper", appMapper);
        ReflectionTestUtils.setField(service, "mapper", attachmentMapper);
        loginUser = User.builder().id(100L).build();
    }

    @Test
    void uploadShouldValidateNullFileBeforeReadingFileName() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.upload(null, null, loginUser)
        );

        assertEquals("上传的文件不能为空", exception.getMessage());
    }

    @Test
    void uploadShouldValidatePdfHeaderRegardlessOfExtensionCase() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "spec.PDF",
                "application/pdf",
                "not a pdf".getBytes(StandardCharsets.UTF_8)
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.upload(file, null, loginUser)
        );

        assertEquals("文件内容不是有效的 PDF", exception.getMessage());
        verify(documentParserManager, never()).documentParse(any());
    }

    @Test
    void uploadShouldStoreNormalizedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "spec.PDF",
                "application/pdf",
                "%PDF-test".getBytes(StandardCharsets.UTF_8)
        );
        when(documentParserManager.documentParse(file)).thenReturn("document content");
        doReturn(true).when(service).save(any(PromptAttachment.class));

        PromptAttachmentVO result = service.upload(file, null, loginUser);

        assertEquals("pdf", result.getFileExtension());
    }

    @Test
    void physicalDeleteExpiredTemporaryShouldDelegateToMapper() {
        LocalDateTime expireTime = LocalDateTime.of(2026, 9, 27, 8, 0);
        when(attachmentMapper.physicalDeleteExpiredTemporary(
                AttachmentStatusEnum.TEMPORARY.getValue(),
                expireTime
        )).thenReturn(2);

        int deletedCount = service.physicalDeleteExpiredTemporary(expireTime);

        assertEquals(2, deletedCount);
        verify(attachmentMapper).physicalDeleteExpiredTemporary(
                AttachmentStatusEnum.TEMPORARY.getValue(),
                expireTime
        );
    }

    @Test
    void validateTemporaryAttachmentShouldOnlySelectId() {
        when(attachmentMapper.selectOneByQuery(any()))
                .thenReturn(PromptAttachment.builder().id(10L).build());

        service.validateTemporaryAttachment(10L, 100L);

        String sql = capturedSelectOneSql();
        assertTrue(sql.contains("select id"));
        assertFalse(sql.contains("content"));
    }

    @Test
    void getAttachmentVOByIdShouldOnlyLoadSummaryFields() {
        PromptAttachment attachment = PromptAttachment.builder()
                .id(10L)
                .fileName("requirements.md")
                .fileExtension("md")
                .content("large content should not be selected")
                .build();
        when(attachmentMapper.selectOneByQuery(any())).thenReturn(attachment);

        PromptAttachmentVO result = service.getAttachmentVOById(10L);

        assertEquals("requirements.md", result.getFileName());
        String sql = capturedSelectOneSql();
        assertTrue(sql.contains("select id, filename, fileextension, contenttype, filesize"));
        assertFalse(sql.contains("select id, filename, fileextension, content,"));
    }

    @Test
    void getAttachmentVOMapByIdsShouldBatchLoadSummaries() {
        PromptAttachment attachment = PromptAttachment.builder()
                .id(10L)
                .fileName("requirements.md")
                .build();
        when(attachmentMapper.selectListByQuery(any())).thenReturn(List.of(attachment));

        Map<Long, PromptAttachmentVO> result =
                service.getAttachmentVOMapByIds(Set.of(10L));

        assertEquals("requirements.md", result.get(10L).getFileName());
        verify(attachmentMapper).selectListByQuery(any());
    }

    private String capturedSelectOneSql() {
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(attachmentMapper).selectOneByQuery(captor.capture());
        return captor.getValue().toSQL().toLowerCase();
    }
}
