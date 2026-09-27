package com.mikle.zerologic.generation.codegen.facade;

import cn.hutool.json.JSONUtil;
import com.mikle.zerologic.generation.codegen.service.AiCodeGeneratorService;
import com.mikle.zerologic.generation.codegen.service.AiCodeGeneratorServiceFactory;
import com.mikle.zerologic.generation.stream.model.AiResponseMessage;
import com.mikle.zerologic.generation.stream.model.ToolExecutedMessage;
import com.mikle.zerologic.generation.stream.model.ToolRequestMessage;
import com.mikle.zerologic.generation.tool.execution.ToolExecutionContext;
import com.mikle.zerologic.generation.tool.execution.ToolExecutionContextHolder;
import com.mikle.zerologic.generation.tool.model.enums.ToolCallSourceEnum;
import com.mikle.zerologic.generation.codegen.parser.CodeParserExecutor;
import com.mikle.zerologic.generation.codegen.saver.CodeFileSaverExecutor;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.generation.codegen.model.enums.CodeGenTypeEnum;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;

/**
 * AI 代码生成门面类，组合代码生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum,
                                                  Long appId, Long taskId, Long userId,
                                                  ToolCallSourceEnum callSource,
                                                  ChatMemory chatMemory) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        // 在订阅时创建 AI Service 和模型流，避免重复订阅共享单次使用的 TokenStream。
        return Flux.defer(() -> {
            AiCodeGeneratorService aiCodeGeneratorService =
                    aiCodeGeneratorServiceFactory.createAiCodeGeneratorService(codeGenTypeEnum, chatMemory);
            return switch (codeGenTypeEnum) {
                case HTML -> processCodeStream(
                        aiCodeGeneratorService.generateHtmlCodeStream(userMessage), codeGenTypeEnum, appId);
                case MULTI_FILE -> processCodeStream(
                        aiCodeGeneratorService.generateMultiFileCodeStream(userMessage), codeGenTypeEnum, appId);
                case VUE_PROJECT -> {
                    TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(userMessage);
                    ToolExecutionContext context = ToolExecutionContext.builder()
                            .taskId(taskId)
                            .appId(appId)
                            .userId(userId)
                            .callSource(callSource)
                            .build();
                    yield processTokenStream(tokenStream, appId, context);
                }
            };
        });
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId, ToolExecutionContext context) {
        return Flux.create(sink -> {
            ToolExecutionContextHolder.set(context);
            sink.onDispose(() -> ToolExecutionContextHolder.clear(appId));
            try {
                tokenStream.onPartialResponse((String partialResponse) -> {
                            if (!sink.isCancelled()) {
                                sink.next(JSONUtil.toJsonStr(new AiResponseMessage(partialResponse)));
                            }
                        })
                        .onPartialToolCall(partialToolCall -> {
                            if (!sink.isCancelled()) {
                                ToolRequestMessage message = new ToolRequestMessage(
                                        partialToolCall.id(),
                                        partialToolCall.name(),
                                        partialToolCall.partialArguments()
                                );
                                sink.next(JSONUtil.toJsonStr(message));
                            }
                        })
                        .onToolExecuted((ToolExecution toolExecution) -> {
                            if (!sink.isCancelled()) {
                                sink.next(JSONUtil.toJsonStr(new ToolExecutedMessage(toolExecution)));
                            }
                        })
                        .onCompleteResponse((ChatResponse response) -> sink.complete())
                        .onError((Throwable error) -> {
                            log.error("Vue 项目生成失败，appId={}", appId, error);
                            sink.error(error);
                        })
                        .start();
            } catch (RuntimeException e) {
                log.error("启动 Vue 项目生成流失败，appId={}", appId, e);
                sink.error(e);
            }
        });
    }

    /**
     * 通用流式代码处理方法
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        return Flux.defer(() -> {
            StringBuilder codeBuilder = new StringBuilder();
            Mono<Void> saveCode = Mono.fromRunnable(() -> {
                        String completeCode = codeBuilder.toString();
                        Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                        File saveDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                        log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .then();
            return codeStream
                    .doOnNext(codeBuilder::append)
                    .concatWith(saveCode.thenMany(Flux.<String>empty()));
        });
    }
}
