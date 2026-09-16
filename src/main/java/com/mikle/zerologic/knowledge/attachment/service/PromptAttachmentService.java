package com.mikle.zerologic.knowledge.attachment.service;

import com.mybatisflex.core.service.IService;
import com.mikle.zerologic.knowledge.attachment.model.entity.PromptAttachment;
import com.mikle.zerologic.user.model.entity.User;
import com.mikle.zerologic.knowledge.attachment.model.vo.PromptAttachmentVO;
import org.springframework.web.multipart.MultipartFile;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/Code-Mikle">Mikle</a>
 */
public interface PromptAttachmentService extends IService<PromptAttachment> {

    PromptAttachmentVO upload(MultipartFile file, Long appId, User loginUser);

    PromptAttachment getUsableAttachment(Long attachmentId, Long userId, Long appId);

    void bindToApp(Long attachmentId, Long appId, Long userId);

    PromptAttachmentVO getAttachmentVOByAppId(Long appId);

    PromptAttachment getTemporaryAttachment(Long attachmentId, Long userId);

    PromptAttachmentVO getAttachmentVOById(Long attachmentId);
}
