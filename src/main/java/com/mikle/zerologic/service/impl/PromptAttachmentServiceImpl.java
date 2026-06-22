package com.mikle.zerologic.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mikle.zerologic.document.DocumentParserManager;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.exception.ThrowUtils;
import com.mikle.zerologic.mapper.AppMapper;
import com.mikle.zerologic.model.entity.App;
import com.mikle.zerologic.model.entity.PromptAttachment;
import com.mikle.zerologic.mapper.PromptAttachmentMapper;
import com.mikle.zerologic.model.entity.User;
import com.mikle.zerologic.model.enums.AttachmentStatusEnum;
import com.mikle.zerologic.model.vo.PromptAttachmentVO;
import com.mikle.zerologic.service.PromptAttachmentService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.mikle.zerologic.constant.PromptLimitConstant.MAX_ATTACHMENT_CONTENT_LENGTH;

/**
 *  服务层实现。
 * @author <a href="https://github.com/Code-Mikle">Mikle</a>
 */
@Service
public class PromptAttachmentServiceImpl extends ServiceImpl<PromptAttachmentMapper, PromptAttachment>  implements PromptAttachmentService{

    @Resource
    private DocumentParserManager documentParserManager;

    @Resource
    private AppMapper appMapper;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;

    private static final int MAX_FILE_NAME_LENGTH = 255;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("txt", "md", "markdown", "pdf");

    @Override
    public PromptAttachmentVO upload(MultipartFile file, Long appId, User loginUser) {
        String fileName = file.getOriginalFilename();
        String extension = FileUtil.extName(fileName);
        validateUploadRequest(file, appId, loginUser);

        if ("pdf".equals(extension)) {
            validatePdfHeader(file);
        }

        String documentContent = documentParserManager.documentParse(file);

        ThrowUtils.throwIf(
                StrUtil.isBlank(documentContent),
                ErrorCode.PARAMS_ERROR,
                "附件未提取到有效文本"
        );

        ThrowUtils.throwIf(
                documentContent.length() > MAX_ATTACHMENT_CONTENT_LENGTH,
                ErrorCode.PARAMS_ERROR,
                "附件提取文本不能超过 20000 字"
        );

        PromptAttachment attachment = PromptAttachment.builder()
                .fileName(fileName)
                .fileExtension(extension)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .content(documentContent)
                .userId(loginUser.getId())
                .appId(appId)
                .status(appId == null
                        ? AttachmentStatusEnum.TEMPORARY.getValue() : AttachmentStatusEnum.BOUND.getValue())
                .build();

        // 执行成功后，会自动回填 attachment 的 id 属性
        boolean saved = this.save(attachment);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "附件记录保存失败");
        return toPromptAttachmentVO(attachment);
    }

    @Override
    public PromptAttachment getUsableAttachment(Long attachmentId, Long userId, Long appId) {
        ThrowUtils.throwIf(attachmentId == null || userId == null || appId == null,
                ErrorCode.OPERATION_ERROR, "附件参数错误");

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", attachmentId)
                .eq("userId", userId)
                .eq("appId", appId)
                .eq("status", AttachmentStatusEnum.BOUND.getValue());

        PromptAttachment attachment = this.getOne(queryWrapper);

        ThrowUtils.throwIf(
                attachment == null,
                ErrorCode.NOT_FOUND_ERROR,
                "附件不存在、无权访问或未绑定到当前应用"
        );

        return attachment;
    }

    /**
     * 主页转到对话页，获得 appId，需要在 prompt_attachment 表中绑定 appId 值
     */
    @Override
    public void bindToApp(Long attachmentId, Long appId, Long userId) {
        ThrowUtils.throwIf(
                attachmentId == null || appId == null || userId == null, ErrorCode.PARAMS_ERROR,
                "附件绑定参数错误"
        );
        PromptAttachment updateAttachment = PromptAttachment.builder()
                .appId(appId)
                .status(AttachmentStatusEnum.BOUND.getValue())
                .build();

        // MyBatis-Flex 的写法
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", attachmentId)
                .eq("userId", userId)
                .eq("status", AttachmentStatusEnum.TEMPORARY.getValue())
                .isNull("appId");
        boolean updated = this.update(updateAttachment, queryWrapper);

        ThrowUtils.throwIf(
                !updated,
                ErrorCode.PARAMS_ERROR,
                "附件不存在、无权访问或已经被绑定"
        );
    }

    @Override
    public PromptAttachmentVO getAttachmentVOByAppId(Long appId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId)
                .eq("status", AttachmentStatusEnum.BOUND.getValue());
        PromptAttachment attachment = this.getOne(queryWrapper);
        return attachment == null ? null : toPromptAttachmentVO(attachment);
    }

    @Override
    public PromptAttachment getTemporaryAttachment(Long attachmentId, Long userId) {
        ThrowUtils.throwIf(
                attachmentId == null || userId == null,
                ErrorCode.PARAMS_ERROR,
                "附件参数错误"
        );

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", attachmentId)
                .eq("userId", userId)
                .eq("status", AttachmentStatusEnum.TEMPORARY.getValue())
                .isNull("appId");

        PromptAttachment attachment = this.getOne(queryWrapper);

        ThrowUtils.throwIf(
                attachment == null,
                ErrorCode.PARAMS_ERROR,
                "附件不存在、无权访问或已经绑定"
        );

        return attachment;
    }

    @Override
    public PromptAttachmentVO getAttachmentVOById(Long attachmentId) {
        if (attachmentId == null) {
            return null;
        }
        PromptAttachment attachment = this.getById(attachmentId);
        return attachment == null ? null : toPromptAttachmentVO(attachment);

    }

    private void validateUploadRequest(MultipartFile file, Long appId, User loginUser) {

        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);


        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR,
                "上传的文件不能为空");

        ThrowUtils.throwIf(file.getSize() > MAX_FILE_SIZE,
                ErrorCode.PARAMS_ERROR, "文件不能超过 5 MB");

        String fileName = file.getOriginalFilename();

        ThrowUtils.throwIf(
                StrUtil.isBlank(fileName),
                ErrorCode.PARAMS_ERROR,
                "文件名不能为空"
        );

        ThrowUtils.throwIf(
                fileName.length() > MAX_FILE_NAME_LENGTH,
                ErrorCode.PARAMS_ERROR,
                "文件名不能超过 255 个字符"
        );

        ThrowUtils.throwIf(
                fileName.contains("..")
                        || fileName.contains("/")
                        || fileName.contains("\\")
                        || fileName.indexOf('\0') >= 0,
                ErrorCode.PARAMS_ERROR,
                "文件名不合法"
        );

        String extension = FileUtil.extName(fileName).toLowerCase();

        ThrowUtils.throwIf(
                !ALLOWED_EXTENSIONS.contains(extension),
                ErrorCode.PARAMS_ERROR,
                "仅支持 TXT、Markdown 和 PDF 文件"
        );

        if (appId != null) {
            validateAppOwnership(appId, loginUser.getId());
        }

    }

    private void validateAppOwnership(Long appId, Long userId) {
        ThrowUtils.throwIf(
                appId <= 0,
                ErrorCode.PARAMS_ERROR,
                "应用 ID 错误"
        );

        App app = appMapper.selectOneById(appId);

        ThrowUtils.throwIf(
                app == null,
                ErrorCode.NOT_FOUND_ERROR,
                "应用不存在"
        );

        ThrowUtils.throwIf(
                !Objects.equals(app.getUserId(), userId),
                ErrorCode.NO_AUTH_ERROR,
                "无权向该应用上传附件"
        );
    }

    private void validatePdfHeader(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(5);

            boolean valid = header.length == 5
                    && header[0] == '%'
                    && header[1] == 'P'
                    && header[2] == 'D'
                    && header[3] == 'F'
                    && header[4] == '-';

            ThrowUtils.throwIf(
                    !valid,
                    ErrorCode.PARAMS_ERROR,
                    "文件内容不是有效的 PDF"
            );
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCode.OPERATION_ERROR,
                    "读取 PDF 文件失败"
            );
        }
    }

    private PromptAttachmentVO toPromptAttachmentVO(PromptAttachment attachment) {
        PromptAttachmentVO vo = new PromptAttachmentVO();
        vo.setId(attachment.getId());
        vo.setFileName(attachment.getFileName());
        vo.setFileExtension(attachment.getFileExtension());
        vo.setContentType(attachment.getContentType());
        vo.setFileSize(attachment.getFileSize());
        return vo;
    }
}
