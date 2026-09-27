package com.mikle.zerologic.knowledge.attachment.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mikle.zerologic.knowledge.attachment.model.entity.PromptAttachment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 *  映射层。
 *
 * @author <a href="https://github.com/Code-Mikle">Mikle</a>
 */
public interface PromptAttachmentMapper extends BaseMapper<PromptAttachment> {

    @Delete("""
            delete from prompt_attachment
            where status = #{status}
              and appId is null
              and createTime < #{expireTime}
            """)
    int physicalDeleteExpiredTemporary(@Param("status") String status,
                                       @Param("expireTime") LocalDateTime expireTime);
}
