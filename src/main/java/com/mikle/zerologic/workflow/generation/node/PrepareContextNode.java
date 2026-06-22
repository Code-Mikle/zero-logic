package com.mikle.zerologic.workflow.generation.node;

import cn.hutool.core.util.StrUtil;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.workflow.generation.GenerationWorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Component
public class PrepareContextNode {

    public AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            GenerationWorkflowContext context = GenerationWorkflowContext.getContext(state);

            if (context == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成工作流上下文不存在");
            }
            if (context.getAppId() == null || context.getAppId() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用 ID 错误");
            }
            if (StrUtil.isBlank(context.getMessage())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "提示词不能为空");
            }
            if (context.getCodeGenType() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
            }

            context.setCurrentStep("prepare_context");
            log.info("生成工作流节点完成: prepare_context, appId={}, codeGenType={}",
                    context.getAppId(), context.getCodeGenType().getValue());

            return GenerationWorkflowContext.saveContext(context);
        });
    }
}
