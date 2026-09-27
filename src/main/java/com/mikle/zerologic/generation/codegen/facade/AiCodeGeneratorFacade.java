package com.mikle.zerologic.generation.codegen.facade;

import cn.hutool.json.JSONUtil;
import com.mikle.zerologic.generation.codegen.service.AiCodeGeneratorService;
import com.mikle.zerologic.generation.codegen.service.AiCodeGeneratorServiceFactory;
import com.mikle.zerologic.generation.memory.service.ChatMemoryProviderService;
import com.mikle.zerologic.generation.stream.model.AiResponseMessage;
import com.mikle.zerologic.generation.stream.model.ToolExecutedMessage;
import com.mikle.zerologic.generation.stream.model.ToolRequestMessage;
import com.mikle.zerologic.generation.tool.execution.ToolExecutionContext;
import com.mikle.zerologic.generation.tool.execution.ToolExecutionContextHolder;
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

import java.io.File;

/**
 * AI 代码生成门面类，组合代码生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private ChatMemoryProviderService chatMemoryProviderService;

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        return generateAndSaveCodeStream(userMessage, codeGenTypeEnum, appId, null, null, null);
    }

    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum,
                                                  Long appId, Long taskId, Long userId, String callSource) {
        ChatMemory chatMemory = chatMemoryProviderService.createAppMemory(appId);
        return generateAndSaveCodeStream(userMessage, codeGenTypeEnum, appId, taskId,
                userId, callSource, chatMemory);
    }

    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum,
                                                  Long appId, Long taskId, Long userId, String callSource,
                                                  ChatMemory chatMemory) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        AiCodeGeneratorService aiCodeGeneratorService =
                aiCodeGeneratorServiceFactory.createAiCodeGeneratorService(codeGenTypeEnum, chatMemory);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
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
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @param appId       应用 ID
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId, ToolExecutionContext context) {
        return Flux.create(sink -> {
            ToolExecutionContextHolder.set(context);
            sink.onDispose(() -> ToolExecutionContextHolder.clear(appId));
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolCall(partialToolCall -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(
                                partialToolCall.id(),
                                partialToolCall.name(),
                                partialToolCall.partialArguments()
                        );
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        ToolExecutionContextHolder.clear(appId);
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        ToolExecutionContextHolder.clear(appId);
                        log.error("Vue 项目生成失败，appId={}", appId, error);
                        sink.error(error);
                    })
                    .start();
        });
    }

    /**
     * 通用流式代码处理方法
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        // 字符串拼接器，用于当流式返回所有的代码之后，再保存代码
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream
                .doOnNext(codeBuilder::append)
                .concatWith(Flux.defer(() -> {
                String completeCode = codeBuilder.toString();
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                File saveDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
                    return Flux.empty();
                }));
    }
}
