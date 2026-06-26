package com.mikle.zerologic.controller;

import cn.hutool.json.JSONUtil;
import com.mikle.zerologic.common.BaseResponse;
import com.mikle.zerologic.common.ResultUtils;
import com.mikle.zerologic.model.dto.generationtask.GenerationTaskCreateRequest;
import com.mikle.zerologic.model.entity.User;
import com.mikle.zerologic.model.vo.GenerationTaskVO;
import com.mikle.zerologic.ratelimter.annotation.RateLimit;
import com.mikle.zerologic.ratelimter.enums.RateLimitType;
import com.mikle.zerologic.service.GenerationTaskService;
import com.mikle.zerologic.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/generation/task")
public class GenerationTaskController {

    @Resource
    private GenerationTaskService generationTaskService;

    @Resource
    private UserService userService;

    @PostMapping("/create")
    @RateLimit(limitType = RateLimitType.USER, rate = 5, rateInterval = 60, message = "AI 对话请求过于频繁，请稍后再试")
    public BaseResponse<Long> createTask(
            @RequestBody GenerationTaskCreateRequest request,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(generationTaskService.createGenerateTask(request, loginUser));
    }

    @GetMapping("/{taskId}")
    public BaseResponse<GenerationTaskVO> getTask(
            @PathVariable Long taskId,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(generationTaskService.getTaskVO(taskId, loginUser));
    }

    @GetMapping(value = "/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamTask(
            @PathVariable Long taskId,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        Flux<String> contentFlux = generationTaskService.streamGenerateTask(taskId, loginUser);
        Flux<ServerSentEvent<String>> tokenFlux = contentFlux.switchOnFirst((signal, stream) -> {
            Flux<ServerSentEvent<String>> tokens = stream.map(chunk -> ServerSentEvent.<String>builder()
                    .data(JSONUtil.toJsonStr(Map.of("d", chunk)))
                    .build());
            if (!signal.hasValue()) {
                return tokens;
            }
            GenerationTaskVO taskVO = generationTaskService.getTaskVO(taskId, loginUser);
            if (taskVO.getRagRetrieval() == null) {
                return tokens;
            }
            ServerSentEvent<String> ragReferences = ServerSentEvent.<String>builder()
                    .event("rag-references")
                    .data(JSONUtil.toJsonStr(taskVO.getRagRetrieval()))
                    .build();
            return Flux.concat(Flux.just(ragReferences), tokens);
        });

        return Flux.concat(
                tokenFlux,
                Mono.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("")
                        .build())
        );
    }

    @PostMapping("/{taskId}/cancel")
    public BaseResponse<Boolean> cancelTask(
            @PathVariable Long taskId,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(generationTaskService.cancelTask(taskId, loginUser));
    }
}
