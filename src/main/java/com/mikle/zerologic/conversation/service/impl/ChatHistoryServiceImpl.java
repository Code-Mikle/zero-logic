package com.mikle.zerologic.conversation.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mikle.zerologic.user.constant.UserConstant;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.exception.ThrowUtils;
import com.mikle.zerologic.conversation.mapper.ChatHistoryMapper;
import com.mikle.zerologic.conversation.model.dto.ChatHistoryQueryRequest;
import com.mikle.zerologic.app.model.entity.App;
import com.mikle.zerologic.conversation.model.entity.ChatHistory;
import com.mikle.zerologic.knowledge.attachment.model.entity.PromptAttachment;
import com.mikle.zerologic.user.model.entity.User;
import com.mikle.zerologic.conversation.model.enums.ChatHistoryMessageTypeEnum;
import com.mikle.zerologic.conversation.model.vo.ChatHistoryVo;
import com.mikle.zerologic.knowledge.attachment.model.vo.PromptAttachmentVO;
import com.mikle.zerologic.knowledge.retrieval.model.vo.RagRetrievalVO;
import com.mikle.zerologic.app.service.AppService;
import com.mikle.zerologic.conversation.service.ChatHistoryService;
import com.mikle.zerologic.knowledge.attachment.service.PromptAttachmentService;
import com.mikle.zerologic.knowledge.retrieval.service.RagRetrievalLogQueryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 对话历史 服务层实现。
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    private static final Set<String> CHAT_HISTORY_SORT_FIELDS = Set.of(
            "id", "messageType", "appId", "userId", "createTime", "updateTime"
    );

    @Resource
    @Lazy
    private AppService appService;

    @Resource
    private PromptAttachmentService promptAttachmentService;

    @Resource
    private RagRetrievalLogQueryService ragRetrievalLogQueryService;

    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId, Long attachmentId, Long taskId) {
        // 基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        // 验证消息类型是否有效
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的消息类型");
        // 插入数据库
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .attachmentId(attachmentId)
                .taskId(taskId)
                .build();
        return this.save(chatHistory);
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }


    @Override
    public Page<ChatHistoryVo> listAppChatHistoryByPage(Long appId,
                                                      int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");

        // 查询 chat_history 分页数据
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        Page<ChatHistory> chatHistoryPage = this.page(Page.of(1, pageSize), queryWrapper);
        List<ChatHistory> chatHistoryPageRecords = chatHistoryPage.getRecords();

        // 提取 attachmentId
        List<Long> attachmentIdList = chatHistoryPageRecords.stream()
                .map(ChatHistory::getAttachmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 批量查询 prompt_attachment
        Map<Long, PromptAttachment> attachmentMap = new HashMap<>();
        if (!attachmentIdList.isEmpty()) {
            List<PromptAttachment> promptAttachmentList = promptAttachmentService.listByIds(attachmentIdList);
            attachmentMap = promptAttachmentList.stream()
                    .collect(Collectors.toMap(
                            PromptAttachment::getId, // 等价于 attachment -> attachment.getId()
                            attachment -> attachment // 表示 Map 的 value 从哪里来，value 就是当前这个附件对象本身
                    ));
        }

        List<Long> taskIdList = chatHistoryPageRecords.stream()
                .map(ChatHistory::getTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, RagRetrievalVO> ragRetrievalMap =
                ragRetrievalLogQueryService.listByTaskIds(taskIdList, appId, app.getUserId());
        List<ChatHistoryVo> chatHistoryVoList = new ArrayList<>();

        for (ChatHistory chatHistory : chatHistoryPageRecords) {
            ChatHistoryVo vo = new ChatHistoryVo();
            vo.setId(chatHistory.getId());
            vo.setTaskId(chatHistory.getTaskId());
            vo.setMessage(chatHistory.getMessage());
            vo.setMessageType(chatHistory.getMessageType());
            vo.setCreateTime(chatHistory.getCreateTime());
            if (ChatHistoryMessageTypeEnum.AI.getValue().equals(chatHistory.getMessageType())
                    && chatHistory.getTaskId() != null) {
                vo.setRagRetrieval(ragRetrievalMap.get(chatHistory.getTaskId()));
            }

            Long attachmentId = chatHistory.getAttachmentId();

            if (attachmentId != null) {
                PromptAttachment attachment = attachmentMap.get(attachmentId);

                if (attachment != null) {
                    PromptAttachmentVO attachmentVO = new PromptAttachmentVO();
                    attachmentVO.setId(attachment.getId());
                    attachmentVO.setFileName(attachment.getFileName());
                    attachmentVO.setFileExtension(attachment.getFileExtension());
                    attachmentVO.setContentType(attachment.getContentType());
                    attachmentVO.setFileSize(attachment.getFileSize());

                    vo.setPromptAttachmentVO(attachmentVO);
                }
            }
            chatHistoryVoList.add(vo);
        }
        // 构造 VO 分页结果
        Page<ChatHistoryVo> voPage = Page.of(chatHistoryPage.getPageNumber(), chatHistoryPage.getPageSize());
        voPage.setRecords(chatHistoryVoList);
        voPage.setTotalPage(chatHistoryPage.getTotalPage());
        voPage.setTotalRow(chatHistoryPage.getTotalRow());

        return voPage;
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        ThrowUtils.throwIf(appId == null || appId <= 0,
                ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(chatMemory == null,
                ErrorCode.PARAMS_ERROR, "聊天记忆不能为空");
        ThrowUtils.throwIf(maxCount <= 0,
                ErrorCode.PARAMS_ERROR, "加载消息数量必须大于 0");

        try {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .orderBy(ChatHistory::getId, false)
                    .limit(maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);

            // 数据库是完整历史的事实来源；查询成功后先清理 Redis 中可能残留的旧记忆
            chatMemory.clear();
            // 反转列表，确保按照时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按照时间顺序将消息添加到记忆中
            int loadedCount = 0;
            for (ChatHistory history : historyList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                } else {
                    log.warn("忽略不支持的历史消息类型, appId={}, chatHistoryId={}, messageType={}",
                            appId, history.getId(), history.getMessageType());
                    continue;
                }
                loadedCount++;
            }
            log.info("成功为 appId: {} 加载 {} 条历史消息", appId, loadedCount);
            return loadedCount;
        } catch (RuntimeException e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }

    /**
     * 获取查询包装类
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        if (StrUtil.isNotBlank(messageType)) {
            ThrowUtils.throwIf(ChatHistoryMessageTypeEnum.getEnumByValue(messageType) == null,
                    ErrorCode.PARAMS_ERROR, "不支持的消息类型");
        }
        // 拼接查询条件
        queryWrapper.eq("id", id, id != null)
                .like("message", message, StrUtil.isNotBlank(message))
                .eq("messageType", messageType, StrUtil.isNotBlank(messageType))
                .eq("appId", appId, appId != null)
                .eq("userId", userId, userId != null);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        sortField = StrUtil.blankToDefault(sortField, "createTime");
        ThrowUtils.throwIf(!CHAT_HISTORY_SORT_FIELDS.contains(sortField),
                ErrorCode.PARAMS_ERROR, "不支持的排序字段");
        ThrowUtils.throwIf(!"ascend".equals(sortOrder) && !"descend".equals(sortOrder),
                ErrorCode.PARAMS_ERROR, "不支持的排序方向");
        boolean ascending = "ascend".equals(sortOrder);
        queryWrapper.orderBy(sortField, ascending);
        if (!"id".equals(sortField)) {
            queryWrapper.orderBy("id", ascending);
        }
        return queryWrapper;
    }
}
