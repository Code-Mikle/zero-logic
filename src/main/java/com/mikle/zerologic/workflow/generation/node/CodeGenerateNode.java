package com.mikle.zerologic.workflow.generation.node;

import cn.hutool.core.util.StrUtil;
import com.mikle.zerologic.core.AiCodeGeneratorFacade;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.service.GenerationTaskProgressService;
import com.mikle.zerologic.workflow.generation.GenerationWorkflowContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Component
public class CodeGenerateNode {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private GenerationTaskProgressService taskProgressService;

    public AsyncNodeAction<MessagesState<String>> create(AtomicReference<Flux<String>> codeStreamRef) {
        return node_async(state -> {
            GenerationWorkflowContext context = GenerationWorkflowContext.getContext(state);

            if (context == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成工作流上下文不存在");
            }

            log.info("生成工作流节点开始: code_generate, appId={}, codeGenType={}",
                    context.getAppId(), context.getCodeGenType().getValue());

            String message = StrUtil.blankToDefault(
                    context.getAssembledMessage(),
                    context.getMessage()
            );
            Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(
                    message,
                    context.getCodeGenType(),
                    context.getAppId(),
                    context.getTaskId(),
                    context.getUserId(),
                    "generate"
            );

            codeStreamRef.set(codeStream);

            context.setCurrentStep("code_generate");
            taskProgressService.updateStep(context.getTaskId(), "code_generate");

            log.info("生成工作流节点完成: code_generate, appId={}", context.getAppId());

            return GenerationWorkflowContext.saveContext(context);
        });
    }
}
